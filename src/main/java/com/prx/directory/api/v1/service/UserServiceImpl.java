package com.prx.directory.api.v1.service;

import com.prx.commons.constants.types.MessageType;
import com.prx.commons.exception.StandardException;
import com.prx.directory.api.v1.to.UseGetResponse;
import com.prx.directory.api.v1.to.UserCreateRequest;
import com.prx.directory.api.v1.to.UserCreateResponse;
import com.prx.directory.client.backbone.BackboneClient;
import com.prx.directory.client.backbone.to.BackboneTokenRequest;
import com.prx.directory.client.mercury.MercuryClient;
import com.prx.directory.kafka.producer.EmailMessageProducerService;
import com.prx.directory.kafka.to.EmailMessageTO;
import com.prx.directory.kafka.to.Recipient;
import com.prx.directory.mapper.UserCreateMapper;
import com.prx.directory.mapper.UserGetMapper;
import com.prx.security.to.AuthRequest;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;

import static com.prx.directory.constant.DirectoryAppConstants.MESSAGE_ERROR_HEADER;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/// Service implementation for user-related operations.
@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String USERNAME_ALREADY_EXISTS = "Username {} already exists";
    private static final String EMAIL_ALREADY_EXISTS = "Email already exists";
    private static final String ERROR_CREATING_USER = "Error creating user";
    private static final int MAX_ALIAS_LENGTH = 12;
    private static final int MAX_ALIAS_TRY = 5;

    @Value("${prx.verification.code.template.id}")
    private UUID verificationCodeTemplateId;
    @Value("${prx.directory.application-id}")
    private String applicationId;
    @Value("${prx.directory.role-id}")
    private String initialRoleId;

    private final EmailMessageProducerService emailMessageProducerService;
    private final UserCreateMapper userCreateMapper;
    private final BackboneClient backboneClient;
    private final UserGetMapper userGetMapper;
    private final MercuryClient mercuryClient;

    /// Constructs a new UserServiceImpl with the specified BackboneClient and UserCreateMapper.
    ///
    /// @param backboneClient   the client used to communicate with the backend
    /// @param userCreateMapper the mapper used to convert between request/response objects and backend objects
    public UserServiceImpl(BackboneClient backboneClient, EmailMessageProducerService emailMessageProducerService, UserCreateMapper userCreateMapper, UserGetMapper userGetMapper, MercuryClient mercuryClient) {
        this.emailMessageProducerService = emailMessageProducerService;
        this.userCreateMapper = userCreateMapper;
        this.backboneClient = backboneClient;
        this.userGetMapper = userGetMapper;
        this.mercuryClient = mercuryClient;
    }

    @Override
    public ResponseEntity<UserCreateResponse> create(UserCreateRequest userCreateRequest) {
        logger.debug("Creating user: {}", userCreateRequest);
        UUID applicationID = UUID.fromString(applicationId);
        try {
            if (Objects.isNull(userCreateRequest)) {
                logger.warn("Content null invalid");
                return ResponseEntity.status(BAD_REQUEST).header(MESSAGE_ERROR_HEADER, "Content null invalid").build();
            }
            backboneClient.checkEmail(userCreateRequest.email(), applicationID);
            var backboneUserCreateRequest = userCreateMapper.toBackbone(userCreateRequest,
                    applicationID, UUID.fromString(initialRoleId), generateAlias(userCreateRequest, true, 1));
            logger.debug("Creating user: {}", backboneUserCreateRequest);
            var userCreateResponse = userCreateMapper.fromBackbone(backboneClient.post(backboneUserCreateRequest));
            emailMessageProducerService.sendMessage(toEmailMessageTO(userCreateRequest, userCreateResponse));
            return ResponseEntity.status(HttpStatus.CREATED).body(userCreateResponse);
        } catch (FeignException e) {
            logger.warn("Error creating user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).header(MESSAGE_ERROR_HEADER, EMAIL_ALREADY_EXISTS).build();
        } catch (StandardException e) {
            logger.warn("Standard Error creating user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).header(MESSAGE_ERROR_HEADER, ERROR_CREATING_USER).build();
        }
    }

    @Override
    public ResponseEntity<UseGetResponse> findUser(UUID id) {
        try {
            var result = backboneClient.find(id);
            return ResponseEntity.ok(userGetMapper.toBackbone(result));
        } catch (FeignException e) {
            logger.warn("Error finding user: {}", e.getMessage());
            if (e.status() == HttpStatus.NOT_FOUND.value()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).header("message-error", "User not found.").build();
            }
            return ResponseEntity.status(e.status()).build();
        }
    }

    @Override
    public ResponseEntity<Boolean> checkVerificationCode(UUID userId) {
        try {
            var userResponse = backboneClient.find(userId);
            BackboneTokenRequest backboneAuthRequest = new BackboneTokenRequest(
                    userResponse.email(), userResponse.password(), userResponse.applications().getFirst().getId());
            AuthRequest mercuryAuthRequest = new AuthRequest(userResponse.alias(), userResponse.password());
            var backboneToken = backboneClient.token(backboneAuthRequest);
            var mercuryToken = mercuryClient.token(backboneToken.token(), mercuryAuthRequest);
            var response = mercuryClient.isVerificationCodeDone(mercuryToken.token(), userId.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.warn("Error checking verification code status for user {}: {}", userId, e.getMessage());
            return ResponseEntity.ok(false);
        }
    }

    private String generateAlias(UserCreateRequest userCreateRequest, boolean afterFirstTime, int time) {
        SecureRandom random = new SecureRandom();
        String alias;
        StringBuilder aliasTemp = new StringBuilder(userCreateRequest.firstname().substring(0, 1)
                .concat(userCreateRequest.lastname()));

        random.nextBytes(new byte[20]);
        if (afterFirstTime) {
            if (aliasTemp.length() >= MAX_ALIAS_LENGTH) {
                aliasTemp.delete(MAX_ALIAS_LENGTH - 5, MAX_ALIAS_LENGTH);
            }
            aliasTemp.append(random.nextInt());
        }
        alias = fixAlias(aliasTemp);
        try {
            backboneClient.checkAlias(alias, UUID.fromString(applicationId));
            return alias.toLowerCase(Locale.ROOT);
        } catch (FeignException e) {
            logger.warn(USERNAME_ALREADY_EXISTS, alias);
        }

        if (time <= MAX_ALIAS_TRY) {
            return generateAlias(userCreateRequest, false, time + 1);
        }
        throw new StandardException("Alias generation failed", MessageType.DEFAULT_MESSAGE);
    }

    private String fixAlias(StringBuilder alias) {
        if (alias.length() >= MAX_ALIAS_LENGTH) {
            return alias.substring(0, MAX_ALIAS_LENGTH - 1);
        }
        return alias.toString();
    }

    private EmailMessageTO toEmailMessageTO(UserCreateRequest userCreateRequest, UserCreateResponse userCreateResponse) {
        String fullname = userCreateRequest.firstname().concat(" ").concat(userCreateRequest.lastname());
        return new EmailMessageTO(verificationCodeTemplateId,
                userCreateResponse.id(),
                "support@latinhub.info",
                List.of(new Recipient(
                        fullname,
                        userCreateResponse.email(),
                        userCreateRequest.firstname())),
                Collections.emptyList(),
                "Verification Code subscription",
                "Your verification code format is: ####-####",
                userCreateResponse.createdDate(),
                Map.of("vc", generateVerificationCode(), "user_name", fullname)
        );
    }

    private String generateVerificationCode() {
        // Implement your code generation logic here
        return UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT) +
                "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
    }
}
