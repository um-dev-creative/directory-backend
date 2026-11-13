package com.prx.directory.services;

import com.prx.directory.client.backbone.BackboneClient;
import com.prx.directory.client.mercury.MercuryClient;
import com.prx.directory.client.mercury.MercuryTokens;
import com.prx.security.service.SessionJwtService;
import com.prx.security.to.AuthRequest;
import com.prx.security.to.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    private final SessionJwtService sessionJwtService = mock(SessionJwtService.class);
    private final BackboneClient backboneClient = mock(BackboneClient.class);
    private final MercuryClient mercuryClient = mock(MercuryClient.class);
    private final MercuryTokens mercuryTokens = mock(MercuryTokens.class);
    private final AuthServiceImpl authService = new AuthServiceImpl(sessionJwtService, backboneClient, mercuryClient, mercuryTokens);

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

    @SuppressWarnings("unchecked")
    private void runsCallsWithTheMercuryToken() {
        when(mercuryTokens.withToken(any())).thenAnswer(inv -> ((java.util.function.Function<String, Object>) inv.getArgument(0)).apply("mercury"));
    }

    @Test
    @DisplayName("token with the backbone session reads the verification status with Mercury's M2M token")
    void tokenWithBackboneSessionAddsTheVerificationStatus() {
        var user = java.util.UUID.randomUUID();
        runsCallsWithTheMercuryToken();
        when(mercuryClient.isVerificationCodeDone("mercury", user)).thenReturn(true);
        when(sessionJwtService.generateSessionToken(anyString(), anyMap())).thenReturn("jwt");

        var response = authService.token(new AuthRequest("alias", ""), backboneTokenOf(user));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(sessionJwtService).generateSessionToken(eq("alias"), argThat(p -> "true".equals(p.get("vcCompleted")) && user.toString().equals(p.get("uid"))));
    }

    @Test
    @DisplayName("token with the backbone session marks the verification incomplete when Mercury has no code")
    void tokenWithBackboneSessionMarksIncompleteOnNotFound() {
        var user = java.util.UUID.randomUUID();
        when(mercuryTokens.withToken(any())).thenThrow(new feign.FeignException.NotFound("nf",
                feign.Request.create(feign.Request.HttpMethod.GET, "http://x", java.util.Map.of(), null, java.nio.charset.StandardCharsets.UTF_8, null), new byte[0], java.util.Map.of()));
        when(sessionJwtService.generateSessionToken(anyString(), anyMap())).thenReturn("jwt");

        var response = authService.token(new AuthRequest("alias", ""), backboneTokenOf(user));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(sessionJwtService).generateSessionToken(eq("alias"), argThat(p -> "false".equals(p.get("vcCompleted"))));
    }

    @Test
    @DisplayName("token with the backbone session rejects a blank alias without touching Mercury")
    void tokenWithBackboneSessionRejectsBlankAlias() {
        assertEquals(HttpStatus.BAD_REQUEST, authService.token(new AuthRequest("", ""), "x").getStatusCode());
        verifyNoInteractions(mercuryClient, mercuryTokens);
    }
}
