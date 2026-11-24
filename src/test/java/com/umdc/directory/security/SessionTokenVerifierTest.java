package com.umdc.directory.security;

import com.umdc.security.jwt.JwtConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SessionTokenVerifier")
class SessionTokenVerifierTest {

    private static final String SECRET = "bG9jYWxEZXZTZWNyZXRLZXlGb3JEaXJlY3RvcnlOb3RGb3JQcm9kdWN0aW9uVXNlMTIzNDU2";
    private static final String OTHER = "b3RoZXJTZWNyZXRLZXlGb3JEaXJlY3RvcnlOb3RGb3JQcm9kdWN0aW9uVXNlMTIzNDU2Nzg5";

    private SessionJwtServiceImpl jwt;
    private SessionTokenVerifier verifier;
    private final UUID uid = UUID.randomUUID();

    private static SessionJwtServiceImpl service(String secret, long ttl) {
        var p = new JwtConfigProperties();
        p.setSecret(secret);
        p.setExpirationMs(ttl);
        return new SessionJwtServiceImpl(p);
    }

    @BeforeEach
    void setUp() {
        jwt = service(SECRET, 60_000L);
        verifier = new SessionTokenVerifier(jwt, true);
    }

    @Test
    @DisplayName("a token signed with the key gives its uid")
    void validToken() {
        String token = jwt.generateSessionToken("alopez", Map.of("uid", uid.toString()));
        assertEquals(uid, verifier.uid(token));
        assertEquals(uid.toString(), verifier.bucketKey(token));
        assertTrue(verifier.verifies());
    }

    @Test
    @DisplayName("a token without uid gives no uid; the bucket falls back to the subject")
    void noUid() {
        String token = jwt.generateSessionToken("alopez", Map.of());
        assertNull(verifier.uid(token));
        assertEquals("alopez", verifier.bucketKey(token));
    }

    @Test
    @DisplayName("a uid that is not a UUID gives null")
    void badUid() {
        assertNull(verifier.uid(jwt.generateSessionToken("alopez", Map.of("uid", "nope"))));
    }

    @Test
    @DisplayName("a forged token (right payload, foreign signature) is refused")
    void forgedToken() {
        String forged = service(OTHER, 60_000L).generateSessionToken("alopez", Map.of("uid", uid.toString()));
        assertNull(verifier.uid(forged));
        assertNull(verifier.bucketKey(forged));
    }

    @Test
    @DisplayName("an unsigned token with a uid payload is refused")
    void unsignedToken() {
        var enc = Base64.getUrlEncoder().withoutPadding();
        String token = enc.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + enc.encodeToString(("{\"uid\":\"" + uid + "\"}").getBytes(StandardCharsets.UTF_8)) + ".";
        assertNull(verifier.uid(token));
    }

    @Test
    @DisplayName("an expired token is refused")
    void expiredToken() {
        String token = service(SECRET, -1_000L).generateSessionToken("alopez", Map.of("uid", uid.toString()));
        assertNull(verifier.uid(token));
    }

    @Test
    @DisplayName("null, blank and garbage are refused")
    void garbage() {
        assertNull(verifier.uid(null));
        assertNull(verifier.uid(" "));
        assertNull(verifier.uid("not-a-jwt"));
        assertNull(verifier.bucketKey(null));
        assertNull(verifier.bucketKey(" "));
    }

    @Test
    @DisplayName("with verification off the payload is read unverified (legacy)")
    void legacyMode() {
        var legacy = new SessionTokenVerifier(jwt, false);
        String forged = service(OTHER, 60_000L).generateSessionToken("alopez", Map.of("uid", uid.toString()));
        assertEquals(uid, legacy.uid(forged));
        assertNull(legacy.bucketKey(forged));
        assertFalse(legacy.verifies());
        assertEquals(uid, SessionTokenVerifier.unverified().uid(forged));
    }

    @Test
    @DisplayName("without a key service the verifier cannot verify and behaves as legacy")
    void noService() {
        assertFalse(new SessionTokenVerifier(null, true).verifies());
    }
}
