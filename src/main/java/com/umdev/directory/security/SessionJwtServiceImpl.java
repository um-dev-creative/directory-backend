package com.umdev.directory.security;

import com.umdev.commons.constants.keys.AuthKey;
import com.umdev.security.exception.CertificateSecurityException;
import com.umdev.security.jwt.JwtConfigProperties;
import com.umdev.security.service.SessionJwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.umdev.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * Service class for handling JWT operations related to sessions.
 */
@Service
public class SessionJwtServiceImpl implements SessionJwtService {

    private static final Logger logger = LoggerFactory.getLogger(SessionJwtServiceImpl.class);
    private final JwtConfigProperties jwtConfigProperties;
    private final SecretKey key;

    /**
     * Constructor to initialize SessionJwtService with JwtConfigProperties.
     *
     * @param jwtConfigProperties the configuration properties for JWT
     */
    public SessionJwtServiceImpl(JwtConfigProperties jwtConfigProperties) {
        this.jwtConfigProperties = jwtConfigProperties;
        this.key = generateKey();
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
        // Required
        claims.put(AuthKey.JTI.value, UUID.randomUUID().toString());
        claims.put(AuthKey.TYPE.value, SESSION_TOKEN_KEY);
        claims.put(AuthKey.IAT.value, new Date());
        // Optional
        if (Objects.nonNull(parameters) && !parameters.isEmpty()) {
            claims.putAll(parameters);
        }

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
    private SecretKey generateKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfigProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
