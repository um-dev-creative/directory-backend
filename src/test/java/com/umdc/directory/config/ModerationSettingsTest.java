package com.umdc.directory.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationSettingsTest {

    @AfterEach
    void reset() {
        ModerationSettings.configure(false, "LH_ADMIN", "roles");
    }

    @Test
    void theBeanLoadsThePropertiesIntoTheStaticSettings() {
        new ModerationSettings(true, " MODERATOR ", " perms ");

        assertTrue(ModerationSettings.enabled());
        assertEquals("MODERATOR", ModerationSettings.adminRole());
        assertEquals("perms", ModerationSettings.roleClaim());
    }

    @Test
    void blankOrNullRoleAndClaimFallBackToTheDefaults() {
        ModerationSettings.configure(false, " ", null);

        assertFalse(ModerationSettings.enabled());
        assertEquals("LH_ADMIN", ModerationSettings.adminRole());
        assertEquals("roles", ModerationSettings.roleClaim());
        ModerationSettings.configure(false, null, " ");
        assertEquals("LH_ADMIN", ModerationSettings.adminRole());
        assertEquals("roles", ModerationSettings.roleClaim());
    }
}
