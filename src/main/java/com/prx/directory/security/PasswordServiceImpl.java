package com.prx.directory.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link PasswordService} using BCrypt algorithm.
 * <p>
 * BCrypt is a password hashing function designed for password storage. It includes:
 * <ul>
 *   <li><b>Automatic salting</b>: A random 16-byte salt is generated and embedded in the hash</li>
 *   <li><b>Adaptive cost factor</b>: Configurable work factor to slow down brute-force attacks</li>
 *   <li><b>Protection against Rainbow Tables</b>: Unique salt per password prevents precomputed attacks</li>
 * </ul>
 * </p>
 *
 * @see BCryptPasswordEncoder
 * @see PasswordService
 */
@Service
public class PasswordServiceImpl implements PasswordService {

    /**
     * BCrypt strength/cost factor.
     * Higher values increase security but also increase computation time.
     * Default is 10 (2^10 = 1024 iterations).
     * Recommended range: 10-12 for most applications.
     */
    private static final int BCRYPT_STRENGTH = 10;

    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Constructs a new PasswordServiceImpl with the configured BCrypt strength.
     */
    public PasswordServiceImpl() {
        this.passwordEncoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    /**
     * Hashes a plain text password using BCrypt with automatic salt generation.
     * The salt is automatically generated and embedded in the resulting hash.
     *
     * @param rawPassword the plain text password to hash
     * @return the hashed password in BCrypt format (includes algorithm, cost, salt, and hash)
     * @throws IllegalArgumentException if rawPassword is null
     */
    @Override
    public String hashPassword(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Verifies if a raw password matches a previously hashed password.
     * BCrypt extracts the salt from the encoded password for verification.
     *
     * @param rawPassword the plain text password to verify
     * @param encodedPassword the previously hashed password (BCrypt format)
     * @return true if the passwords match, false otherwise
     */
    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
