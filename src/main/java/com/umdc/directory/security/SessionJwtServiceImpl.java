package com.umdc.directory.security;

import com.umdc.commons.constants.keys.AuthKey;
import com.umdc.security.exception.CertificateSecurityException;
import com.umdc.security.jwt.JwtConfigProperties;
import com.umdc.security.service.SessionJwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * Service class for handling JWT operations related to sessions.
 */
@Service
public class SessionJwtServiceImpl implements SessionJwtService {

    private static final Logger logger = LoggerFactory.getLogger(SessionJwtServiceImpl.class);
    private static final int DEFAULT_MIN_KEY_BYTES = 32;
    /** Claims the caller parameters can never set: they belong to the service. */
    private static final Set<String> RESERVED_CLAIMS = Set.of("jti", "type", "iat", "exp", "nbf", "sub", "iss", "aud");
    private final JwtConfigProperties jwtConfigProperties;
    private final SecretKey key;

    /**
     * Constructor to initialize SessionJwtService with JwtConfigProperties.
     *
     * @param jwtConfigProperties the configuration properties for JWT
     */
    public SessionJwtServiceImpl(JwtConfigProperties jwtConfigProperties) {
        this(jwtConfigProperties, DEFAULT_MIN_KEY_BYTES);
    }

    /**
     * Constructor used by Spring.
     *
     * @param jwtConfigProperties the configuration properties for JWT
     * @param minKeyBytes         minimum HMAC key length in bytes (`umdc.security.jwt.min-key-bytes`, default 32)
     * @throws IllegalStateException when the secret is not Base64 or is shorter than the minimum
     */
    @Autowired
    public SessionJwtServiceImpl(JwtConfigProperties jwtConfigProperties,
                                 @Value("${umdc.security.jwt.min-key-bytes:32}") int minKeyBytes) {
        this.jwtConfigProperties = jwtConfigProperties;
        this.key = generateKey(minKeyBytes);
    }

    /**
     * Generates a session token for the given username.
     *
     * @param username   the username
     * @param parameters the parameters
     * @return the generated session token
     */
    public String generateSessionToken(String username, Map<String, String> parameters) {
        Map<String, Object> claims = new ConcurrentHashMap<>();
        // Optional: applied first and never able to replace the reserved claims below
        if (Objects.nonNull(parameters)) {
            parameters.forEach((name, value) -> {
                if (Objects.nonNull(name) && Objects.nonNull(value) && !RESERVED_CLAIMS.contains(name)) {
                    claims.put(name, value);
                }
            });
        }
        // Required (win over the caller parameters)
        claims.put(AuthKey.JTI.value, UUID.randomUUID().toString());
        claims.put(AuthKey.TYPE.value, SESSION_TOKEN_KEY);
        claims.put(AuthKey.IAT.value, new Date());

        return Jwts.builder().claims(claims).subject(username).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + jwtConfigProperties.getExpirationMs())).signWith(key).compact();
    }

    /**
     * Retrieves the claims from the given token.
     *
     * @param token the JWT token
     * @return the claims contained in the token
     */
    @Override
    public Claims getTokenClaims(String token) throws CertificateSecurityException {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            throw new CertificateSecurityException(e.getMessage(), e);
        }
        return claims;
    }

    /**
     * Retrieves the username from the given token.
     *
     * @param token the JWT token
     * @return the username contained in the token
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    /**
     * Generates a SecretKey from the JWT configuration properties.
     *
     * @return the generated SecretKey
     */
    private SecretKey generateKey(int minKeyBytes) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtConfigProperties.getSecret());
        } catch (RuntimeException e) {
            throw new IllegalStateException("The JWT secret (APP_TOKEN_SECRET) must be Base64 encoded", e);
        }
        if (keyBytes.length < minKeyBytes) {
            throw new IllegalStateException("The JWT secret (APP_TOKEN_SECRET) decodes to " + keyBytes.length
                    + " bytes; at least " + minKeyBytes + " are required (umdc.security.jwt.min-key-bytes)");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
