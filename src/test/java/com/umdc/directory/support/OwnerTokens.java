package com.umdc.directory.support;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/** Builds unsigned JWT-shaped tokens carrying a {@code uid} claim, enough for JwtUtil.getUidFromToken. */
public final class OwnerTokens {

    private OwnerTokens() {
    }

    public static String forUser(UUID uid) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        String header = enc.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = enc.encodeToString(("{\"uid\":\"" + uid + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".sig";
    }
}
