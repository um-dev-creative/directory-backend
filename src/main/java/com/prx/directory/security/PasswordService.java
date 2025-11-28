package com.prx.directory.security;

/**
 * Service interface for password hashing and validation.
 * Provides secure password operations using salting and hashing
 * to protect against Rainbow Table and Brute-force attacks.
 */
public interface PasswordService {

    /**
     * Hashes a plain text password using BCrypt with automatic salt generation.
     * Each call generates a unique salt, making the hash resistant to Rainbow Table attacks.
     *
     * @param rawPassword the plain text password to hash
     * @return the hashed password including the salt
     */
    String hashPassword(String rawPassword);

    /**
     * Verifies if a raw password matches a previously hashed password.
     * Extracts the salt from the stored hash and uses it for comparison.
     *
     * @param rawPassword the plain text password to verify
     * @param encodedPassword the previously hashed password (includes salt)
     * @return true if the passwords match, false otherwise
     */
    boolean matches(String rawPassword, String encodedPassword);
}
