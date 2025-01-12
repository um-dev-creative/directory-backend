package com.prx.directory.services;

import com.prx.directory.client.BackboneClient;
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
    private final AuthServiceImpl authService = new AuthServiceImpl(sessionJwtService, backboneClient);

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
}
