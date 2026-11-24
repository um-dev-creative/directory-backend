package com.umdc.directory.api.v1.service;

import com.umdc.directory.config.ModerationSettings;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/// Reads the roles of a session token to tell whether the caller may moderate. The signature is already checked by the session interceptor
/// of the library, like for the `uid` claim ([com.umdc.directory.util.JwtUtil]).
final class ModerationAdmin {

    private static final int MIN_PARTS = 2;

    private ModerationAdmin() {
        throw new IllegalStateException("Utility class");
    }

    /// True when the roles claim (`umdc.moderation.role-claim`) holds the admin role (`umdc.moderation.admin-role`, case-insensitive). The claim
    /// may be a JSON array or a comma or space separated string.
    ///
    /// @param token the session token
    /// @return true for a moderator; false for anything else, including a token that cannot be read
    static boolean isAdmin(String token) {
        try {
            var parts = token.split("\\.");
            if (parts.length < MIN_PARTS) {
                return false;
            }
            var payload = new JSONObject(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
            var claim = payload.opt(ModerationSettings.roleClaim());
            var admin = ModerationSettings.adminRole();
            if (claim instanceof JSONArray array) {
                for (var i = 0; i < array.length(); i++) {
                    if (admin.equalsIgnoreCase(String.valueOf(array.opt(i)).trim())) {
                        return true;
                    }
                }
                return false;
            }
            if (claim instanceof String text) {
                for (var role : text.split("[,\\s]+")) {
                    if (admin.equalsIgnoreCase(role)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
