package com.umdc.directory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// Business moderation settings (S8). The values are static because the public queries (`BusinessSpecifications`, the native
/// geo queries) are built without a Spring context in reach; the bean only loads them from the properties at startup.
///
/// - `umdc.moderation.enabled` (default `false`): new businesses start `PENDING` and the public reads return only `APPROVED` ones.
/// - `umdc.moderation.admin-role` (default `LH_ADMIN`): role that may approve and reject.
/// - `umdc.moderation.role-claim` (default `roles`): claim of the session token that lists the roles.
@Component
public class ModerationSettings {

    private static volatile boolean enabled;
    private static volatile String adminRole = "LH_ADMIN";
    private static volatile String roleClaim = "roles";

    /// @param enabledValue   `umdc.moderation.enabled`
    /// @param adminRoleValue `umdc.moderation.admin-role`
    /// @param roleClaimValue `umdc.moderation.role-claim`
    public ModerationSettings(@Value("${umdc.moderation.enabled:false}") boolean enabledValue,
                              @Value("${umdc.moderation.admin-role:LH_ADMIN}") String adminRoleValue,
                              @Value("${umdc.moderation.role-claim:roles}") String roleClaimValue) {
        configure(enabledValue, adminRoleValue, roleClaimValue);
    }

    /// Sets the three values; a blank role or claim falls back to its default.
    ///
    /// @param enabledValue   whether moderation is on
    /// @param adminRoleValue role that may moderate
    /// @param roleClaimValue claim that lists the roles
    public static void configure(boolean enabledValue, String adminRoleValue, String roleClaimValue) {
        enabled = enabledValue;
        adminRole = adminRoleValue == null || adminRoleValue.isBlank() ? "LH_ADMIN" : adminRoleValue.trim();
        roleClaim = roleClaimValue == null || roleClaimValue.isBlank() ? "roles" : roleClaimValue.trim();
    }

    /// @return true when moderation is on
    public static boolean enabled() {
        return enabled;
    }

    /// @return the role that may approve and reject
    public static String adminRole() {
        return adminRole;
    }

    /// @return the token claim that lists the roles
    public static String roleClaim() {
        return roleClaim;
    }
}
