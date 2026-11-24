package com.umdc.directory.services;

import com.umdc.directory.api.v1.service.MercuryVerificationService;
import com.umdc.directory.client.backbone.BackboneClient;
import com.umdc.directory.client.mercury.MercuryErrors;
import com.umdc.directory.client.mercury.MercuryThrottledException;
import com.umdc.directory.client.mercury.MercuryUnavailableException;
import com.umdc.directory.config.ModerationSettings;
import com.umdc.security.service.AuthService;
import com.umdc.security.service.SessionJwtService;
import com.umdc.security.to.AuthRequest;
import com.umdc.security.to.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.umdc.directory.constant.DirectoryAppConstants.MESSAGE_ERROR_HEADER;
import static com.umdc.directory.util.JwtUtil.getUidFromToken;
import static org.apache.commons.lang3.BooleanUtils.FALSE;

/**
 * Service implementation for authentication-related operations.
 *
 * <p>Security review (S4): the library exposes {@code POST /token} and {@code POST /session-token} without a session token
 * ({@code @SkipSessionValidation}). {@code /token} validates the Backbone token and then signs a JWT for any alias the caller names, with
 * no user id; {@code /session-token} used to read the {@code uid} of the Backbone token without validating it, so a forged token would
 * have been accepted. Both are now restricted, see {@link #token(AuthRequest)} and {@link #token(AuthRequest, String)}.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int MAX_ALIAS_LENGTH = 128;
    private final SessionJwtService sessionJwtService;
    private final BackboneClient backboneClient;
    private final MercuryVerificationService mercuryVerification;

    /** {@code umdc.auth.alias-token.enabled}: keeps the legacy alias-only {@code POST /token} (default true until the decision is taken). */
    @SuppressWarnings("PMD.ImmutableField") // set by Spring and by tests
    @Value("${umdc.auth.alias-token.enabled:true}")
    private boolean aliasTokenEnabled = true;

    /**
     * Constructor for AuthServiceImpl.
     *
     * @param sessionJwtService   the service for generating JWT tokens
     * @param backboneClient      the client for interacting with the backbone service
     * @param mercuryVerification Mercury's verification codes (status of the user's code)
     */
    public AuthServiceImpl(SessionJwtService sessionJwtService, BackboneClient backboneClient,
                           MercuryVerificationService mercuryVerification) {
        this.sessionJwtService = sessionJwtService;
        this.backboneClient = backboneClient;
        this.mercuryVerification = mercuryVerification;
    }

    /**
     * Generates a session token from an alias alone (legacy). The token has no {@code uid}, so it cannot own anything, but it is
     * still a valid session token for whatever alias the caller names: it can be switched off with
     * {@code umdc.auth.alias-token.enabled=false} (answers 403) and the alias is limited to 128 printable characters.
     *
     * @param authRequest the authentication request containing user alias
     * @return ResponseEntity containing the authentication response with the session token
     */
    @Override
    public ResponseEntity<AuthResponse> token(AuthRequest authRequest) {
        if (!aliasTokenEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .header(MESSAGE_ERROR_HEADER, "Alias-only tokens are disabled.").build();
        }
        if (Objects.isNull(authRequest) || !validAlias(authRequest.alias())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        logger.warn("Alias-only session token issued (legacy endpoint)");
        var authResponse = new AuthResponse(sessionJwtService.generateSessionToken(authRequest.alias(), new ConcurrentHashMap<>()));
        if (authResponse.token().isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Generates a session token for the user of a Backbone session token. The Backbone token is validated with Backbone first (the
     * {@code uid} claim is trusted only after that); 401 when it is not valid.
     *
     * @param authRequest     the authentication request containing user alias
     * @param sessionTokenBkd the session token used for backend session validation
     * @return ResponseEntity containing the authentication response with the session token
     */
    @Override
    public ResponseEntity<AuthResponse> token(AuthRequest authRequest, String sessionTokenBkd) {
        if (Objects.isNull(authRequest) || !validAlias(authRequest.alias())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (!isBackboneSessionValid(sessionTokenBkd)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(MESSAGE_ERROR_HEADER, "Invalid Backbone session token.").build();
        }
        UUID userId = getUidFromToken(sessionTokenBkd);
        if (Objects.isNull(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(MESSAGE_ERROR_HEADER, "The session token has no user.").build();
        }
        var parameters = new ConcurrentHashMap<String, String>();
        parameters.put("uid", userId.toString());
        try {
            parameters.put("vcCompleted", String.valueOf(mercuryVerification.isVerified(userId)));
        } catch (MercuryThrottledException | MercuryUnavailableException e) {
            logger.warn("Mercury could not report the verification of user {}: {}", userId, e.getClass().getSimpleName());
            return MercuryErrors.toResponse(e);
        }
        if (parameters.get("vcCompleted").equals(FALSE)) {
            logger.info("Verification code not completed for user {}:{}", userId, authRequest.alias());
        }
        addRoles(parameters, userId);
        var authResponse = new AuthResponse(sessionJwtService.generateSessionToken(authRequest.alias(), parameters));
        if (authResponse.token().isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        return ResponseEntity.ok(authResponse);
    }

    private boolean isBackboneSessionValid(String sessionTokenBkd) {
        if (Objects.isNull(sessionTokenBkd) || sessionTokenBkd.isBlank()) {
            return false;
        }
        try {
            return backboneClient.validate(sessionTokenBkd);
        } catch (RuntimeException e) {
            logger.warn("Backbone could not validate the session token: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    /** With moderation on, the roles of the user go in the claim the moderation admin check reads; best effort. */
    private void addRoles(ConcurrentHashMap<String, String> parameters, UUID userId) {
        if (!ModerationSettings.enabled()) {
            return;
        }
        try {
            var user = backboneClient.findUserById(userId);
            if (Objects.nonNull(user) && Objects.nonNull(user.roles())) {
                parameters.put(ModerationSettings.roleClaim(), user.roles().stream()
                        .map(com.umdc.commons.general.pojo.Role::getName).filter(Objects::nonNull).collect(Collectors.joining(",")));
            }
        } catch (RuntimeException e) {
            logger.warn("The roles of user {} could not be read: {}", userId, e.getClass().getSimpleName());
        }
    }

    private static boolean validAlias(String alias) {
        return Objects.nonNull(alias) && !alias.isBlank() && alias.length() <= MAX_ALIAS_LENGTH
                && alias.chars().noneMatch(Character::isISOControl);
    }

    /**
     * Validates the provided session token using the backbone client.
     *
     * @param sessionTokenBkd the session token to validate
     * @return true if the session token is valid, false otherwise
     */
    @Override
    public boolean validate(String sessionTokenBkd) {
        return backboneClient.validate(sessionTokenBkd);
    }
}
