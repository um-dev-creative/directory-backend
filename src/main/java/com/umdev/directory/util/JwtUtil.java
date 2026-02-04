package com.umdev.directory.util;

import org.json.JSONObject;

import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public final class JwtUtil {
    private static final int MAX_LENGTH = 2;

    private JwtUtil(){
        // Default constructor
    }

    /**
     * Extracts the uid from a JWT token.
     *
     * @param token the JWT token
     * @return the uid if present, otherwise null
     */

    public static UUID getUidFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length < MAX_LENGTH) {
                return null;
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            JSONObject payload = new JSONObject(payloadJson);
            var tempo = payload.optString("uid", null);

            return Objects.nonNull(tempo) ? UUID.fromString(tempo) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
