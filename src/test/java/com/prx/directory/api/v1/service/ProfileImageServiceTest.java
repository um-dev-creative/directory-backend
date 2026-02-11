package com.prx.directory.api.v1.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProfileImageService.
 */
class ProfileImageServiceTest {

    @Test
    @DisplayName("ProfileImageService: default implementation save doesn't throw")
    void testSaveDefaultImplementation() {
        ProfileImageService service = new ProfileImageService() {};
        assertDoesNotThrow(() -> service.save("appId", new byte[0]));
    }
}
