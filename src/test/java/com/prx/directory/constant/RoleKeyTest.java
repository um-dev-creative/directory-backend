package com.prx.directory.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RoleKey enum - contains expected values")
class RoleKeyTest {

    @Test
    @DisplayName("enum contains LH_STANDARD")
    void containsStandard() {
        boolean found = false;
        for (RoleKey r : RoleKey.values()) {
            if (r.name().equals("LH_STANDARD")) found = true;
        }
        assertTrue(found);
    }
}

