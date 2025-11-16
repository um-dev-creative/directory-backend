package com.prx.directory.util;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static String makeToken(String payloadJson) {
        String header = "{}";
        String headerB = Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
        String payloadB = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes());
        return headerB + "." + payloadB + ".sig";
    }

    @Test
    void getUidFromToken_nullToken_returnsNull() {
        assertNull(JwtUtil.getUidFromToken(null));
    }

    @Test
    void getUidFromToken_blankToken_returnsNull() {
        assertNull(JwtUtil.getUidFromToken("   \t"));
    }

    @Test
    void getUidFromToken_tooFewParts_returnsNull() {
        assertNull(JwtUtil.getUidFromToken("abc"));
        assertNull(JwtUtil.getUidFromToken("a.b")); // exactly two parts? parts.length==2 is OK; but check 'a' only
    }

    @Test
    void getUidFromToken_invalidBase64_returnsNull() {
        String token = "a." + "!!not-base64!!" + ".c";
        assertNull(JwtUtil.getUidFromToken(token));
    }

    @Test
    void getUidFromToken_missingUid_returnsNull() {
        String payload = "{\"sub\":\"x\"}";
        String token = makeToken(payload);
        assertNull(JwtUtil.getUidFromToken(token));
    }

    @Test
    void getUidFromToken_emptyUid_returnsNull() {
        String payload = "{\"uid\": \"\"}";
        String token = makeToken(payload);
        assertNull(JwtUtil.getUidFromToken(token));
    }

    @Test
    void getUidFromToken_invalidUuid_returnsNull() {
        String payload = "{\"uid\": \"not-a-uuid\"}";
        String token = makeToken(payload);
        assertNull(JwtUtil.getUidFromToken(token));
    }

    @Test
    void getUidFromToken_validUuid_returnsUuid() {
        UUID id = UUID.randomUUID();
        String payload = "{\"uid\": \"" + id.toString() + "\"}";
        String token = makeToken(payload);
        UUID result = JwtUtil.getUidFromToken(token);
        assertNotNull(result);
        assertEquals(id, result);
    }
}

