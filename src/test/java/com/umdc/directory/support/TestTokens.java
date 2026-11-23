package com.umdc.directory.support;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/** Builds unsigned JWT-shaped session tokens whose payload carries the {@code uid} claim, for unit tests. */
public final class TestTokens {

    private TestTokens() {
    }

    /** Token whose uid claim is {@code userId}. */
    public static String forUser(UUID userId) {
        var enc = Base64.getUrlEncoder().withoutPadding();
        var header = enc.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        var payload = enc.encodeToString(("{\"uid\":\"" + userId + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".sig";
    }
}
