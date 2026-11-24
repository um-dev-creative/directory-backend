package com.umdc.directory.services;

import com.umdc.directory.client.backbone.BackboneClient;
import com.umdc.directory.api.v1.service.MercuryVerificationService;
import com.umdc.security.service.SessionJwtService;
import com.umdc.security.to.AuthRequest;
import com.umdc.security.to.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    private final SessionJwtService sessionJwtService = mock(SessionJwtService.class);
    private final BackboneClient backboneClient = mock(BackboneClient.class);
    private final MercuryVerificationService mercuryVerification = mock(MercuryVerificationService.class);
    private final AuthServiceImpl authService = new AuthServiceImpl(sessionJwtService, backboneClient, mercuryVerification);

    @Test
    @DisplayName("token should return OK status with valid alias")
    void tokenShouldReturnOkStatusWithValidAlias() {
        AuthRequest authRequest = new AuthRequest("validAlias", "");
        when(sessionJwtService.generateSessionToken(anyString(), anyMap())).thenReturn("validToken");

        ResponseEntity<AuthResponse> response = authService.token(authRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("validToken", response.getBody().token());
    }

    @Test
    @DisplayName("token should return BAD_REQUEST status with null alias")
    void tokenShouldReturnBadRequestStatusWithNullAlias() {
        AuthRequest authRequest = new AuthRequest(null, null);

        ResponseEntity<AuthResponse> response = authService.token(authRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("token should return BAD_REQUEST status with blank alias")
    void tokenShouldReturnBadRequestStatusWithBlankAlias() {
        AuthRequest authRequest = new AuthRequest("", "");

        ResponseEntity<AuthResponse> response = authService.token(authRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("token should return NOT_ACCEPTABLE status with blank token")
    void tokenShouldReturnNotAcceptableStatusWithBlankToken() {
        AuthRequest authRequest = new AuthRequest("validAlias", "");
        when(sessionJwtService.generateSessionToken(anyString(), anyMap())).thenReturn("");

        ResponseEntity<AuthResponse> response = authService.token(authRequest);

        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
    }

    @Test
    @DisplayName("validate should return true for valid token")
    void validateShouldReturnTrueForValidToken() {
        String sessionTokenBkd = "validToken";
        when(backboneClient.validate(sessionTokenBkd)).thenReturn(true);

        boolean isValid = authService.validate(sessionTokenBkd);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("validate should return false for invalid token")
    void validateShouldReturnFalseForInvalidToken() {
        String sessionTokenBkd = "invalidToken";
        when(backboneClient.validate(sessionTokenBkd)).thenReturn(false);

        boolean isValid = authService.validate(sessionTokenBkd);

        assertFalse(isValid);
    }

    private static String backboneTokenOf(java.util.UUID uid) {
        var payload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"uid\":\"" + uid + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "h." + payload + ".s";
    }

    @Test
    @DisplayName("token with the backbone session reads the verification status from Mercury")
    void tokenWithBackboneSessionAddsTheVerificationStatus() {
        var user = java.util.UUID.randomUUID();
        when(backboneClient.validate(anyString())).thenReturn(true);
        when(mercuryVerification.isVerified(user)).thenReturn(true);
        when(sessionJwtService.generateSessionToken(anyString(), anyMap())).thenReturn("jwt");

        var response = authService.token(new AuthRequest("alias", ""), backboneTokenOf(user));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(sessionJwtService).generateSessionToken(eq("alias"), argThat(p -> "true".equals(p.get("vcCompleted")) && user.toString().equals(p.get("uid"))));
    }

    @Test
    @DisplayName("token with the backbone session marks the verification incomplete (and keeps the uid) when Mercury has no code")
    void tokenWithBackboneSessionMarksIncomplete() {
        var user = java.util.UUID.randomUUID();
        when(backboneClient.validate(anyString())).thenReturn(true);
        when(mercuryVerification.isVerified(user)).thenReturn(false);
        when(sessionJwtService.generateSessionToken(anyString(), anyMap())).thenReturn("jwt");

        var response = authService.token(new AuthRequest("alias", ""), backboneTokenOf(user));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(sessionJwtService).generateSessionToken(eq("alias"), argThat(p -> "false".equals(p.get("vcCompleted")) && user.toString().equals(p.get("uid"))));
    }

    @Test
    @DisplayName("token with the backbone session rejects a blank or null alias without touching Backbone or Mercury")
    void tokenWithBackboneSessionRejectsBlankAlias() {
        assertEquals(HttpStatus.BAD_REQUEST, authService.token(new AuthRequest("", ""), "x").getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, authService.token(new AuthRequest(null, ""), "x").getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, authService.token(null, "x").getStatusCode());
        verifyNoInteractions(backboneClient, mercuryVerification);
    }

    @Test
    @DisplayName("S4: a backbone token that Backbone does not validate gets 401 and no session token")
    void tokenWithAForgedBackboneSessionIsRefused() {
        var user = java.util.UUID.randomUUID();
        when(backboneClient.validate(anyString())).thenReturn(false);

        var response = authService.token(new AuthRequest("alias", ""), backboneTokenOf(user));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(sessionJwtService, mercuryVerification);
    }

    @Test
    @DisplayName("S4: a missing, blank or unvalidatable backbone token gets 401")
    void tokenWithoutAUsableBackboneSessionIsRefused() {
        assertEquals(HttpStatus.UNAUTHORIZED, authService.token(new AuthRequest("alias", ""), null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, authService.token(new AuthRequest("alias", ""), " ").getStatusCode());
        when(backboneClient.validate("boom")).thenThrow(new IllegalStateException("backbone down"));
        assertEquals(HttpStatus.UNAUTHORIZED, authService.token(new AuthRequest("alias", ""), "boom").getStatusCode());
        verifyNoInteractions(sessionJwtService);
    }

    @Test
    @DisplayName("a validated backbone token without uid gets 401")
    void tokenWithoutUidIsRefused() {
        when(backboneClient.validate(anyString())).thenReturn(true);

        assertEquals(HttpStatus.UNAUTHORIZED, authService.token(new AuthRequest("alias", ""), "h.e30.s").getStatusCode());
    }

    @Test
    @DisplayName("a throttled Mercury is answered 429 with Retry-After and no session token")
    void mercuryThrottleIsSurfaced() {
        var user = java.util.UUID.randomUUID();
        when(backboneClient.validate(anyString())).thenReturn(true);
        when(mercuryVerification.isVerified(user)).thenThrow(new com.umdc.directory.client.mercury.MercuryThrottledException(42));

        var response = authService.token(new AuthRequest("alias", ""), backboneTokenOf(user));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("42", response.getHeaders().getFirst("Retry-After"));
        assertNotNull(response.getHeaders().getFirst("message-error"));
        verifyNoInteractions(sessionJwtService);
    }

    @Test
    @DisplayName("an unavailable Mercury is answered 503")
    void mercuryUnavailableIsSurfaced() {
        var user = java.util.UUID.randomUUID();
        when(backboneClient.validate(anyString())).thenReturn(true);
        when(mercuryVerification.isVerified(user)).thenThrow(new com.umdc.directory.client.mercury.MercuryUnavailableException("down"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, authService.token(new AuthRequest("alias", ""), backboneTokenOf(user)).getStatusCode());
    }

    @Test
    @DisplayName("a blank session token from the signer is NOT_ACCEPTABLE")
    void blankSignedTokenIsNotAcceptable() {
        var user = java.util.UUID.randomUUID();
        when(backboneClient.validate(anyString())).thenReturn(true);
        when(sessionJwtService.generateSessionToken(anyString(), anyMap())).thenReturn("");

        assertEquals(HttpStatus.NOT_ACCEPTABLE, authService.token(new AuthRequest("alias", ""), backboneTokenOf(user)).getStatusCode());
    }

    @Test
    @DisplayName("S4: the alias-only token can be switched off (403) and rejects control characters and over-long aliases")
    void aliasOnlyTokenIsRestricted() {
        assertEquals(HttpStatus.BAD_REQUEST, authService.token(new AuthRequest("a\nb", "")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, authService.token(new AuthRequest("x".repeat(129), "")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, authService.token((AuthRequest) null).getStatusCode());

        org.springframework.test.util.ReflectionTestUtils.setField(authService, "aliasTokenEnabled", false);
        var response = authService.token(new AuthRequest("validAlias", ""));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getHeaders().getFirst("message-error"));
        verifyNoInteractions(sessionJwtService);
    }

    @Test
    @DisplayName("with moderation on the roles of the user go in the roles claim; a Backbone failure only skips the claim")
    void rolesClaimWithModeration() {
        var user = java.util.UUID.randomUUID();
        var role = new com.umdc.commons.general.pojo.Role();
        role.setName("LH_ADMIN");
        var backboneUser = new com.umdc.directory.client.backbone.to.BackboneUserGetResponse(user, "a", null, "a@x.com", "A", null, null,
                true, false, false, false, null, java.util.List.of(role), null);
        when(backboneClient.validate(anyString())).thenReturn(true);
        when(mercuryVerification.isVerified(user)).thenReturn(true);
        when(sessionJwtService.generateSessionToken(anyString(), anyMap())).thenReturn("jwt");
        com.umdc.directory.config.ModerationSettings.configure(true, "LH_ADMIN", "roles");
        try {
            when(backboneClient.findUserById(user)).thenReturn(backboneUser);
            authService.token(new AuthRequest("alias", ""), backboneTokenOf(user));
            verify(sessionJwtService).generateSessionToken(eq("alias"), argThat(p -> "LH_ADMIN".equals(p.get("roles"))));

            doThrow(new IllegalStateException("down")).when(backboneClient).findUserById(user);
            assertEquals(HttpStatus.OK, authService.token(new AuthRequest("alias", ""), backboneTokenOf(user)).getStatusCode());

            doReturn(null).when(backboneClient).findUserById(user);
            assertEquals(HttpStatus.OK, authService.token(new AuthRequest("alias", ""), backboneTokenOf(user)).getStatusCode());
        } finally {
            com.umdc.directory.config.ModerationSettings.configure(false, "LH_ADMIN", "roles");
        }
    }

    @Test
    @DisplayName("with moderation off Backbone is not asked for roles")
    void noRolesClaimWithoutModeration() {
        var user = java.util.UUID.randomUUID();
        when(backboneClient.validate(anyString())).thenReturn(true);
        when(sessionJwtService.generateSessionToken(anyString(), anyMap())).thenReturn("jwt");

        authService.token(new AuthRequest("alias", ""), backboneTokenOf(user));

        verify(backboneClient, never()).findUserById(any());
    }
}
