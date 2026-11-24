package com.umdc.directory.security;

import com.umdc.security.exception.CertificateSecurityException;
import com.umdc.security.jwt.JwtConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SessionJwtServiceImpl")
class SessionJwtServiceImplTest {

    private static final String SECRET = "bG9jYWxEZXZTZWNyZXRLZXlGb3JEaXJlY3RvcnlOb3RGb3JQcm9kdWN0aW9uVXNlMTIzNDU2";

    private SessionJwtServiceImpl service;

    @BeforeEach
    void setUp() {
        var properties = new JwtConfigProperties();
        properties.setSecret(SECRET);
        properties.setExpirationMs(60_000L);
        service = new SessionJwtServiceImpl(properties);
    }

    @Test
    @DisplayName("a token it issued carries the subject and the extra claims, and verifies")
    void issuesAndVerifies() throws CertificateSecurityException {
        String token = service.generateSessionToken("alopez", Map.of("uid", "u-1"));

        var claims = service.getTokenClaims(token);

        assertEquals("alopez", claims.getSubject());
        assertEquals("u-1", claims.get("uid"));
        assertNotNull(claims.getId());
        assertEquals("alopez", service.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("a token with no extra parameters is still valid")
    void issuesWithoutParameters() {
        assertEquals("alopez", service.getUsernameFromToken(service.generateSessionToken("alopez", null)));
    }

    @Test
    @DisplayName("a tampered or foreign token is refused")
    void refusesATamperedToken() {
        String token = service.generateSessionToken("alopez", Map.of());

        assertThrows(CertificateSecurityException.class, () -> service.getTokenClaims(token.substring(0, token.lastIndexOf('.') + 1) + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        assertThrows(CertificateSecurityException.class, () -> service.getTokenClaims("not-a-jwt"));
    }

    @Test
    @DisplayName("jti, type, subject and issue time cannot be overwritten by the caller parameters")
    void reservedClaimsWin() throws CertificateSecurityException {
        String token = service.generateSessionToken("alopez", Map.of("jti", "evil", "type", "refresh", "sub", "root",
                "exp", "1", "iat", "1", "iss", "x", "aud", "y", "nbf", "1", "uid", "u-1"));

        var claims = service.getTokenClaims(token);

        assertNotEquals("evil", claims.getId());
        assertNotEquals("refresh", claims.get("type"));
        assertEquals(com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY, claims.get("type"));
        assertEquals("alopez", claims.getSubject());
        assertEquals("u-1", claims.get("uid"));
        assertNull(claims.getIssuer());
        assertNull(claims.getAudience());
        assertTrue(claims.getExpiration().getTime() > System.currentTimeMillis());
    }

    @Test
    @DisplayName("null values in the parameters are ignored")
    void nullParameterValues() {
        var params = new java.util.HashMap<String, String>();
        params.put("a", null);
        params.put("b", "1");
        assertEquals("alopez", service.getUsernameFromToken(service.generateSessionToken("alopez", params)));
    }

    @Test
    @DisplayName("a key shorter than the minimum fails at startup with a clear message")
    void shortKeyFailsFast() {
        var properties = new JwtConfigProperties();
        properties.setSecret(java.util.Base64.getEncoder().encodeToString("short-key-of-20-bytes".getBytes()));
        var ex = assertThrows(IllegalStateException.class, () -> new SessionJwtServiceImpl(properties));
        assertTrue(ex.getMessage().contains("at least 32"));
    }

    @Test
    @DisplayName("the minimum is configurable")
    void minimumIsConfigurable() {
        var properties = new JwtConfigProperties();
        properties.setSecret(java.util.Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes()));
        assertThrows(IllegalStateException.class, () -> new SessionJwtServiceImpl(properties, 33));
        assertNotNull(new SessionJwtServiceImpl(properties, 32));
    }

    @Test
    @DisplayName("a secret that is not Base64 fails at startup with a clear message")
    void notBase64() {
        var properties = new JwtConfigProperties();
        properties.setSecret("not base64 !!");
        var ex = assertThrows(IllegalStateException.class, () -> new SessionJwtServiceImpl(properties));
        assertTrue(ex.getMessage().contains("Base64"));
    }
}
