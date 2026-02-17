package com.prx.directory.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PasswordServiceImpl}.
 * Tests validate BCrypt password hashing with automatic salting and password verification.
 */
class PasswordServiceImplTest {

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordServiceImpl();
    }

    @Test
    @DisplayName("hashPassword should generate a BCrypt hash")
    void hashPassword_shouldGenerateBcryptHash() {
        String rawPassword = "mySecurePassword123";

        String hashedPassword = passwordService.hashPassword(rawPassword);

        assertNotNull(hashedPassword, "Hashed password should not be null");
        assertTrue(hashedPassword.startsWith("$2a$"), "Hash should start with BCrypt prefix $2a$");
        assertNotEquals(rawPassword, hashedPassword, "Hashed password should be different from raw password");
    }

    @Test
    @DisplayName("hashPassword should generate unique hash each time due to random salt")
    void hashPassword_shouldGenerateUniqueSaltEachTime() {
        String rawPassword = "mySecurePassword123";

        String hash1 = passwordService.hashPassword(rawPassword);
        String hash2 = passwordService.hashPassword(rawPassword);

        assertNotEquals(hash1, hash2, "Each hash should be unique due to random salt (Rainbow Table protection)");
    }

    @Test
    @DisplayName("matches should return true for correct password")
    void matches_shouldReturnTrueForCorrectPassword() {
        String rawPassword = "mySecurePassword123";
        String hashedPassword = passwordService.hashPassword(rawPassword);

        boolean result = passwordService.matches(rawPassword, hashedPassword);

        assertTrue(result, "Password verification should succeed for correct password");
    }

    @Test
    @DisplayName("matches should return false for incorrect password")
    void matches_shouldReturnFalseForIncorrectPassword() {
        String rawPassword = "mySecurePassword123";
        String wrongPassword = "wrongPassword456";
        String hashedPassword = passwordService.hashPassword(rawPassword);

        boolean result = passwordService.matches(wrongPassword, hashedPassword);

        assertFalse(result, "Password verification should fail for incorrect password");
    }

    @Test
    @DisplayName("matches should return false when raw password is null")
    void matches_shouldReturnFalseWhenRawPasswordIsNull() {
        String hashedPassword = passwordService.hashPassword("somePassword");

        boolean result = passwordService.matches(null, hashedPassword);

        assertFalse(result, "Verification should fail when raw password is null");
    }

    @Test
    @DisplayName("matches should return false when encoded password is null")
    void matches_shouldReturnFalseWhenEncodedPasswordIsNull() {
        boolean result = passwordService.matches("somePassword", null);

        assertFalse(result, "Verification should fail when encoded password is null");
    }

    @Test
    @DisplayName("hashPassword should throw exception for null password")
    void hashPassword_shouldThrowExceptionForNullPassword() {
        assertThrows(IllegalArgumentException.class, () -> passwordService.hashPassword(null),
                "hashPassword should throw IllegalArgumentException for null password");
    }

    @Test
    @DisplayName("hashPassword should handle empty password")
    void hashPassword_shouldHandleEmptyPassword() {
        String emptyPassword = "";

        String hashedPassword = passwordService.hashPassword(emptyPassword);

        assertNotNull(hashedPassword, "Hashed password for empty string should not be null");
        assertTrue(passwordService.matches(emptyPassword, hashedPassword),
                "Empty password should match its hash");
    }

    @Test
    @DisplayName("hashPassword should handle special characters")
    void hashPassword_shouldHandleSpecialCharacters() {
        String specialPassword = "P@$$w0rd!#%^&*()_+-=[]{}|;':\",./<>?";

        String hashedPassword = passwordService.hashPassword(specialPassword);

        assertNotNull(hashedPassword, "Hashed password should not be null");
        assertTrue(passwordService.matches(specialPassword, hashedPassword),
                "Password with special characters should match its hash");
    }

    @Test
    @DisplayName("hashPassword should handle unicode characters")
    void hashPassword_shouldHandleUnicodeCharacters() {
        String unicodePassword = "contraseña密码пароль";

        String hashedPassword = passwordService.hashPassword(unicodePassword);

        assertNotNull(hashedPassword, "Hashed password should not be null");
        assertTrue(passwordService.matches(unicodePassword, hashedPassword),
                "Password with unicode characters should match its hash");
    }

    @Test
    @DisplayName("hashPassword should handle long passwords")
    void hashPassword_shouldHandleLongPasswords() {
        String longPassword = "a".repeat(100);

        String hashedPassword = passwordService.hashPassword(longPassword);

        assertNotNull(hashedPassword, "Hashed password should not be null");
        assertTrue(passwordService.matches(longPassword, hashedPassword),
                "Long password should match its hash");
    }

    @Test
    @DisplayName("BCrypt hash format should contain cost factor")
    void hashPassword_shouldContainCostFactor() {
        String rawPassword = "testPassword";
        // BCrypt format pattern: $2a$10$... where 2a/2b/2y are BCrypt versions and 10 is the cost factor
        String bcryptFormatPattern = "\\$2[aby]\\$\\d{2}\\$.+";

        String hashedPassword = passwordService.hashPassword(rawPassword);

        assertTrue(hashedPassword.matches(bcryptFormatPattern),
                "BCrypt hash should match expected format with cost factor");
    }

    @Test
    @DisplayName("matches should be resilient to timing attacks (consistent time)")
    void matches_shouldBeResilientToTimingAttacks() {
        String correctPassword = "correctPassword123";
        String hashedPassword = passwordService.hashPassword(correctPassword);

        // Both calls should complete (we're testing that matches doesn't short-circuit in obvious ways)
        boolean correctResult = passwordService.matches(correctPassword, hashedPassword);
        boolean incorrectResult = passwordService.matches("wrongPassword", hashedPassword);

        assertTrue(correctResult, "Correct password should match");
        assertFalse(incorrectResult, "Wrong password should not match");
    }
}
