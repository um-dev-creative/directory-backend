package com.prx.directory.security;

import com.prx.security.exception.CertificateSecurityException;
import com.prx.security.jwt.JwtConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
