package com.prx.directory.services;

import com.prx.directory.client.backbone.BackboneClient;
import com.prx.directory.client.mercury.MercuryClient;
import com.prx.security.service.AuthService;
import com.prx.security.service.SessionJwtService;
import com.prx.security.to.AuthRequest;
import com.prx.security.to.AuthResponse;
import feign.FeignException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.BooleanUtils.FALSE;

/**
 * Service implementation for authentication-related operations.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private final SessionJwtService sessionJwtService;
    private final BackboneClient backboneClient;
    private final MercuryClient mercuryClient;
    private static final int MAX_LENGTH = 2;

    /**
     * Constructor for AuthServiceImpl.
     *
     * @param sessionJwtService the service for generating JWT tokens
     * @param backboneClient    the client for interacting with the backbone service
     * @param mercuryClient     the client for interacting with the backbone service
     */
    public AuthServiceImpl(SessionJwtService sessionJwtService, BackboneClient backboneClient, MercuryClient mercuryClient) {
        this.sessionJwtService = sessionJwtService;
        this.backboneClient = backboneClient;
        this.mercuryClient = mercuryClient;
    }

    /**
     * Generates a session token based on the provided authentication request.
     *
     * @param authRequest the authentication request containing user alias
     * @return ResponseEntity containing the authentication response with the session token
     */
    @Override
    public ResponseEntity<AuthResponse> token(AuthRequest authRequest) {
        if (Objects.isNull(authRequest.alias()) || authRequest.alias().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        var authResponse = new AuthResponse(sessionJwtService.generateSessionToken(authRequest.alias(), new ConcurrentHashMap<>()));
        if (Objects.isNull(authResponse) || authResponse.token().isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Generates a session token based on the provided authentication request.
     *
     * @param authRequest     the authentication request containing user alias
     * @param sessionTokenBkd the session token used for backend session validation
     * @return ResponseEntity containing the authentication response with the session token
     */
    @Override
    public ResponseEntity<AuthResponse> token(AuthRequest authRequest, String sessionTokenBkd) {
        var parameters = new ConcurrentHashMap<String, String>();
        Boolean verificationCodeCompleted;
        if (authRequest.alias().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        var mercuryToken = mercuryClient.token(sessionTokenBkd, authRequest);
        String userId = getUidFromToken(sessionTokenBkd);
        try {
            verificationCodeCompleted = mercuryClient.isVerificationCodeDone(mercuryToken.token(), userId);
            parameters.put("vcCompleted", verificationCodeCompleted.toString());
        } catch (FeignException.NotFound e) {
            logger.info("Token verification code not found for user {}:{}", userId, authRequest.alias());
            parameters.put("vcCompleted", FALSE);
        }
        var authResponse = new AuthResponse(sessionJwtService.generateSessionToken(authRequest.alias(), parameters));
        if (authResponse.token().isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        return ResponseEntity.ok(authResponse);
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

    /**
     * Extracts the uid from a JWT token.
     *
     * @param token the JWT token
     * @return the uid if present, otherwise null
     */
    public String getUidFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length < MAX_LENGTH) {
                return null;
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            JSONObject payload = new JSONObject(payloadJson);
            return payload.optString("uid", null);
        } catch (Exception e) {
            return null;
        }
    }
}
