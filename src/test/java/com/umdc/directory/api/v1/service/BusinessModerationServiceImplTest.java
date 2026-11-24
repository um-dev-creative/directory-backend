package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.VerificationConfirmRequest;
import com.umdc.directory.client.backbone.BackboneClient;
import com.umdc.directory.client.backbone.to.BackboneUserGetResponse;
import com.umdc.directory.client.mercury.MercuryThrottledException;
import com.umdc.directory.client.mercury.MercuryUnavailableException;
import com.umdc.directory.config.ModerationSettings;
import com.umdc.directory.constant.ModerationStatus;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.support.TestTokens;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BusinessModerationServiceImplTest {

    private final BusinessRepository repository = mock(BusinessRepository.class);
    private final BackboneClient backbone = mock(BackboneClient.class);
    private final MercuryVerificationService mercury = mock(MercuryVerificationService.class);
    private final UUID owner = UUID.randomUUID();
    private final UUID businessId = UUID.randomUUID();
    private final String ownerToken = TestTokens.forUser(owner);
    private final LocalDateTime start = LocalDateTime.of(2026, 1, 1, 12, 0);
    private Clock clock;
    private BusinessModerationServiceImpl service;
    private BusinessEntity business;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(start.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new BusinessModerationServiceImpl(repository, backbone, mercury, clock);
        business = new BusinessEntity();
        business.setId(businessId);
        business.setUserFk(owner);
        business.setModerationStatus(ModerationStatus.PENDING);
        when(repository.findById(businessId)).thenReturn(Optional.of(business));
        when(repository.save(any(BusinessEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void reset() {
        ModerationSettings.configure(false, "LH_ADMIN", "roles");
    }

    private BackboneUserGetResponse ownerUser(String email) {
        return new BackboneUserGetResponse(owner, "alias", null, email, "Owner", null, null, true, false, false, false, null, List.of(), List.of());
    }

    private static String tokenWith(String payloadJson) {
        var enc = Base64.getUrlEncoder().withoutPadding();
        return enc.encodeToString("{}".getBytes(StandardCharsets.UTF_8)) + "." + enc.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8)) + ".s";
    }

    /// Requests a code and returns the one that was sent.
    private String requestCode() {
        when(backbone.findUserById(owner)).thenReturn(ownerUser("owner@x.com"));
        assertEquals(HttpStatus.ACCEPTED, service.requestVerification(businessId, ownerToken).getStatusCode());
        var code = ArgumentCaptor.forClass(String.class);
        verify(mercury).sendCode(any(), anyString(), anyString(), code.capture());
        return code.getValue();
    }

    // --- request ---

    @Test
    void requestSendsA9DigitCodeAndStoresOnlyItsHash() {
        var code = requestCode();

        assertTrue(code.matches("\\d{9}"));
        assertNotNull(business.getVerificationCodeHash());
        assertFalse(business.getVerificationCodeHash().contains(code));
        assertEquals(64, business.getVerificationCodeHash().length());
        assertEquals(start.plus(BusinessModerationServiceImpl.CODE_TTL), business.getVerificationExpiresAt());
        assertEquals(0, business.getVerificationAttempts());
        verify(mercury).sendCode(owner, "owner@x.com", "Owner", code);
    }

    @Test
    void requestNeedsAValidTokenAnExistingBusinessAndTheOwner() {
        assertEquals(HttpStatus.UNAUTHORIZED, service.requestVerification(businessId, null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.requestVerification(businessId, "junk").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.requestVerification(UUID.randomUUID(), ownerToken).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, service.requestVerification(businessId, TestTokens.forUser(UUID.randomUUID())).getStatusCode());
        verifyNoInteractions(mercury);
    }

    @Test
    void aSecondRequestWithinAMinuteIs429WithRetryAfter() {
        requestCode();

        var response = service.requestVerification(businessId, ownerToken);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("60", response.getHeaders().getFirst("Retry-After"));
        verify(mercury, times(1)).sendCode(any(), anyString(), anyString(), anyString());
    }

    @Test
    void aNewRequestIsAllowedAfterTheCooldown() {
        requestCode();
        var later = new BusinessModerationServiceImpl(repository, backbone, mercury,
                Clock.fixed(Instant.parse("2026-01-01T12:02:00Z"), ZoneOffset.UTC));

        assertEquals(HttpStatus.ACCEPTED, later.requestVerification(businessId, ownerToken).getStatusCode());
        verify(mercury, times(2)).sendCode(any(), anyString(), anyString(), anyString());
    }

    @Test
    void requestWithoutOwnerEmailIsConflict() {
        when(backbone.findUserById(owner)).thenReturn(ownerUser(" "));
        assertEquals(HttpStatus.CONFLICT, service.requestVerification(businessId, ownerToken).getStatusCode());
        when(backbone.findUserById(owner)).thenReturn(ownerUser(null));
        assertEquals(HttpStatus.CONFLICT, service.requestVerification(businessId, ownerToken).getStatusCode());
        when(backbone.findUserById(owner)).thenReturn(null);
        assertEquals(HttpStatus.CONFLICT, service.requestVerification(businessId, ownerToken).getStatusCode());
        verifyNoInteractions(mercury);
    }

    @Test
    void requestWhenBackboneFailsIs503() {
        when(backbone.findUserById(owner)).thenThrow(new IllegalStateException("down"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, service.requestVerification(businessId, ownerToken).getStatusCode());
    }

    @Test
    void requestWhenMercuryThrottlesIs429AndNothingIsStored() {
        when(backbone.findUserById(owner)).thenReturn(ownerUser("o@x.com"));
        doThrow(new MercuryThrottledException(33)).when(mercury).sendCode(any(), anyString(), anyString(), anyString());

        var response = service.requestVerification(businessId, ownerToken);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("33", response.getHeaders().getFirst("Retry-After"));
        assertNull(business.getVerificationCodeHash());
        verify(repository, never()).save(any());
    }

    @Test
    void requestWhenMercuryIsDownIs503() {
        when(backbone.findUserById(owner)).thenReturn(ownerUser("o@x.com"));
        doThrow(new MercuryUnavailableException("down")).when(mercury).sendCode(any(), anyString(), anyString(), anyString());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, service.requestVerification(businessId, ownerToken).getStatusCode());
        assertNull(business.getVerificationCodeHash());
    }

    // --- confirm ---

    @Test
    void theRightCodeVerifiesTheBusinessAndClearsTheCode() {
        var code = requestCode();

        var response = service.confirmVerification(businessId, new VerificationConfirmRequest(" " + code + " "), ownerToken);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertTrue(business.isVerified());
        assertNull(business.getVerificationCodeHash());
        assertNull(business.getVerificationExpiresAt());
        assertEquals(0, business.getVerificationAttempts());
        assertEquals(HttpStatus.CONFLICT, service.confirmVerification(businessId, new VerificationConfirmRequest(code), ownerToken).getStatusCode());
    }

    @Test
    void aWrongCodeIs400AndCountsAnAttempt() {
        var code = requestCode();
        var wrong = code.equals("000000000") ? "000000001" : "000000000";

        var response = service.confirmVerification(businessId, new VerificationConfirmRequest(wrong), ownerToken);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(1, business.getVerificationAttempts());
        assertFalse(business.isVerified());
    }

    @Test
    void afterFiveWrongCodesEvenTheRightOneIs429() {
        var code = requestCode();
        var wrong = code.equals("000000000") ? "000000001" : "000000000";
        for (int i = 0; i < BusinessModerationServiceImpl.MAX_ATTEMPTS; i++) {
            assertEquals(HttpStatus.BAD_REQUEST, service.confirmVerification(businessId, new VerificationConfirmRequest(wrong), ownerToken).getStatusCode());
        }

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, service.confirmVerification(businessId, new VerificationConfirmRequest(code), ownerToken).getStatusCode());
        assertFalse(business.isVerified());
    }

    @Test
    void anExpiredCodeIs410AndIsCleared() {
        var code = requestCode();
        var later = new BusinessModerationServiceImpl(repository, backbone, mercury,
                Clock.fixed(Instant.parse("2026-01-01T12:15:00Z"), ZoneOffset.UTC));

        assertEquals(HttpStatus.GONE, later.confirmVerification(businessId, new VerificationConfirmRequest(code), ownerToken).getStatusCode());
        assertNull(business.getVerificationCodeHash());
    }

    @Test
    void aMalformedOrMissingCodeIs400() {
        assertEquals(HttpStatus.BAD_REQUEST, service.confirmVerification(businessId, null, ownerToken).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.confirmVerification(businessId, new VerificationConfirmRequest(null), ownerToken).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.confirmVerification(businessId, new VerificationConfirmRequest("12345"), ownerToken).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.confirmVerification(businessId, new VerificationConfirmRequest("12345678a"), ownerToken).getStatusCode());
    }

    @Test
    void confirmWithoutAPendingCodeIs409() {
        assertEquals(HttpStatus.CONFLICT, service.confirmVerification(businessId, new VerificationConfirmRequest("123456789"), ownerToken).getStatusCode());
        business.setVerificationCodeHash("abc");
        assertEquals(HttpStatus.CONFLICT, service.confirmVerification(businessId, new VerificationConfirmRequest("123456789"), ownerToken).getStatusCode());
    }

    @Test
    void confirmNeedsAValidTokenAnExistingBusinessAndTheOwner() {
        var body = new VerificationConfirmRequest("123456789");
        assertEquals(HttpStatus.UNAUTHORIZED, service.confirmVerification(businessId, body, null).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.confirmVerification(UUID.randomUUID(), body, ownerToken).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, service.confirmVerification(businessId, body, TestTokens.forUser(UUID.randomUUID())).getStatusCode());
    }

    @Test
    void theConfirmRequestNeverPrintsTheCode() {
        assertFalse(new VerificationConfirmRequest("123456789").toString().contains("123456789"));
    }

    // --- status ---

    @Test
    void theOwnerSeesTheStatusAndTheVerifiedFlag() {
        business.setVerified(true);

        var response = service.status(businessId, ownerToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ModerationStatus.PENDING, response.getBody().status());
        assertTrue(response.getBody().verified());
        assertEquals(businessId, response.getBody().businessId());
    }

    @Test
    void statusIsOwnerOnly() {
        assertEquals(HttpStatus.UNAUTHORIZED, service.status(businessId, null).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, service.status(UUID.randomUUID(), ownerToken).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, service.status(businessId, TestTokens.forUser(UUID.randomUUID())).getStatusCode());
    }

    // --- approve / reject ---

    @Test
    void anAdminWithTheRoleInAnArrayApprovesAndRejects() {
        var admin = tokenWith("{\"uid\":\"" + UUID.randomUUID() + "\",\"roles\":[\"LH_STANDARD\",\"LH_ADMIN\"]}");

        var approved = service.approve(businessId, admin);
        assertEquals(HttpStatus.OK, approved.getStatusCode());
        assertEquals(ModerationStatus.APPROVED, business.getModerationStatus());
        assertEquals(ModerationStatus.APPROVED, approved.getBody().status());

        var rejected = service.reject(businessId, admin);
        assertEquals(HttpStatus.OK, rejected.getStatusCode());
        assertEquals(ModerationStatus.REJECTED, business.getModerationStatus());
        assertEquals(LocalDateTime.of(2026, 1, 1, 12, 0), business.getLastUpdate());
    }

    @Test
    void theRoleAsACommaOrSpaceSeparatedStringWorksCaseInsensitively() {
        assertTrue(ModerationAdmin.isAdmin(tokenWith("{\"roles\":\"LH_STANDARD, lh_admin\"}")));
        assertTrue(ModerationAdmin.isAdmin(tokenWith("{\"roles\":\"LH_STANDARD LH_ADMIN\"}")));
    }

    @Test
    void theRoleAndTheClaimAreConfigurable() {
        ModerationSettings.configure(true, "MODERATOR", "perms");

        assertTrue(ModerationAdmin.isAdmin(tokenWith("{\"perms\":[\"MODERATOR\"]}")));
        assertFalse(ModerationAdmin.isAdmin(tokenWith("{\"roles\":[\"MODERATOR\"]}")));
        assertFalse(ModerationAdmin.isAdmin(tokenWith("{\"perms\":[\"LH_ADMIN\"]}")));
    }

    @Test
    void aTokenWithoutTheRoleIsForbidden() {
        assertEquals(HttpStatus.FORBIDDEN, service.approve(businessId, ownerToken).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, service.reject(businessId, tokenWith("{\"roles\":[\"LH_STANDARD\"]}")).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, service.approve(businessId, tokenWith("{\"roles\":\"LH_ADMINISTRATOR\"}")).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, service.approve(businessId, tokenWith("{\"roles\":42}")).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, service.approve(businessId, "not-a-jwt").getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, service.approve(businessId, "h.!!!.s").getStatusCode());
        assertEquals(ModerationStatus.PENDING, business.getModerationStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void approveWithoutTokenIs401AndAnUnknownBusinessIs404() {
        assertEquals(HttpStatus.UNAUTHORIZED, service.approve(businessId, null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.reject(businessId, " ").getStatusCode());
        var admin = tokenWith("{\"roles\":[\"LH_ADMIN\"]}");
        assertEquals(HttpStatus.NOT_FOUND, service.approve(UUID.randomUUID(), admin).getStatusCode());
    }

    @Test
    void thePublicConstructorUsesTheSystemClock() {
        var real = new BusinessModerationServiceImpl(repository, backbone, mercury);

        assertEquals(HttpStatus.UNAUTHORIZED, real.status(businessId, null).getStatusCode());
    }
}
