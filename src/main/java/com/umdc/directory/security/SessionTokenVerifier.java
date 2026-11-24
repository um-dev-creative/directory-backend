package com.umdc.directory.security;

import com.umdc.directory.util.JwtUtil;
import com.umdc.security.service.SessionJwtService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/// Single place that turns a `session-token` header into the caller's user id for authorisation decisions.
///
/// With `umdc.security.jwt.verify-signature=true` (default) the token is parsed with the service key: signature and expiry
/// are checked, and a token that fails answers `null` (callers translate that into 401). With `false` it falls back to
/// {@link JwtUtil#getUidFromToken(String)}, which decodes the payload without verifying it (legacy behaviour, only for
/// environments where the key cannot be shared).
@Component
public class SessionTokenVerifier {

    private static final Logger logger = LoggerFactory.getLogger(SessionTokenVerifier.class);
    private static final SessionTokenVerifier UNVERIFIED = new SessionTokenVerifier(null, false);

    private final SessionJwtService sessionJwtService;
    private final boolean verifySignature;

    /// @param sessionJwtService the service that holds the signing key
    /// @param verifySignature   `umdc.security.jwt.verify-signature` (default true)
    @Autowired
    public SessionTokenVerifier(SessionJwtService sessionJwtService,
                                @Value("${umdc.security.jwt.verify-signature:true}") boolean verifySignature) {
        this.sessionJwtService = sessionJwtService;
        this.verifySignature = verifySignature && Objects.nonNull(sessionJwtService);
    }

    /// @return a verifier that never checks the signature (legacy behaviour; default of constructors kept for existing callers)
    public static SessionTokenVerifier unverified() {
        return UNVERIFIED;
    }

    /// @param token value of the `session-token` header
    /// @return the `uid` claim, or null when the token is missing, invalid, expired, forged or has no uid
    public UUID uid(String token) {
        if (!verifySignature) {
            return JwtUtil.getUidFromToken(token);
        }
        Claims claims = claims(token);
        if (claims == null) {
            return null;
        }
        try {
            String uid = claims.get("uid", String.class);
            return uid == null ? null : UUID.fromString(uid);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /// @param token value of the `session-token` header
    /// @return the `uid` claim as text, else `sub`, used only to pick a rate limit bucket; null when absent or unverifiable
    public String bucketKey(String token) {
        if (token == null) {
            return null;
        }
        if (!verifySignature) {
            return null;
        }
        Claims claims = claims(token);
        if (claims == null) {
            return null;
        }
        Object uid = claims.get("uid");
        return uid != null ? uid.toString() : claims.getSubject();
    }

    /// @return true when the signature and expiry are enforced
    public boolean verifies() {
        return verifySignature;
    }

    private Claims claims(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return sessionJwtService.getTokenClaims(token);
        } catch (Exception e) {
            logger.debug("Session token rejected: {}", e.getMessage());
            return null;
        }
    }
}
