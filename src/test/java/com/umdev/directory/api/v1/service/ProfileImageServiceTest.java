package com.umdev.directory.api.v1.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProfileImageService.
 */
class ProfileImageServiceTest {

    @Test
    void testSaveDefaultImplementation() {
        ProfileImageService service = new ProfileImageService() {};
        assertDoesNotThrow(() -> service.save("appId", new byte[0]));
    }
}
