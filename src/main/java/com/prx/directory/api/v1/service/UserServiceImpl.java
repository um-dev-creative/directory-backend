package com.prx.directory.api.v1.service;

import com.prx.commons.constants.httpstatus.type.MessageType;
import com.prx.commons.exception.StandardException;
import com.prx.directory.api.v1.to.GetUserResponse;
import com.prx.directory.api.v1.to.PutUserRequest;
import com.prx.directory.api.v1.to.UserCreateRequest;
import com.prx.directory.api.v1.to.UserCreateResponse;
import com.prx.directory.client.backbone.BackboneClient;
import com.prx.directory.client.backbone.to.BackboneUserUpdateRequest;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.kafka.producer.EmailMessageProducerService;
import com.prx.directory.kafka.to.EmailMessageTO;
import com.prx.directory.kafka.to.Recipient;
import com.prx.directory.mapper.GetUserMapper;
import com.prx.directory.mapper.PutUserMapper;
import com.prx.directory.mapper.UserCreateMapper;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;

import static com.prx.directory.constant.DirectoryAppConstants.*;
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
    private String applicationIdString;
    @Value("${prx.directory.role-id}")
    private String initialRoleId;

    private final EmailMessageProducerService emailMessageProducerService;
    private final UserCreateMapper userCreateMapper;
    private final PutUserMapper putUserMapper;
    private final BackboneClient backboneClient;
    private final GetUserMapper getUserMapper;
    private final BusinessRepository businessRepository;

    /// Constructs a new UserServiceImpl with the specified BackboneClient and UserCreateMapper.
    ///
    /// @param backboneClient   the client used to communicate with the backend
    /// @param userCreateMapper the mapper used to convert between request/response objects and backend objects
    public UserServiceImpl(BackboneClient backboneClient, EmailMessageProducerService emailMessageProducerService,
                           UserCreateMapper userCreateMapper, PutUserMapper putUserMapper,
                           GetUserMapper getUserMapper, BusinessRepository businessRepository) {
        this.emailMessageProducerService = emailMessageProducerService;
        this.userCreateMapper = userCreateMapper;
        this.backboneClient = backboneClient;
        this.putUserMapper = putUserMapper;
        this.getUserMapper = getUserMapper;
        this.businessRepository = businessRepository;
    }

    @Override
    public ResponseEntity<UserCreateResponse> create(UserCreateRequest userCreateRequest) {
        logger.debug("Creating user: {}", userCreateRequest);
        UUID applicationID = UUID.fromString(applicationIdString);
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
    public ResponseEntity<GetUserResponse> findUser(String token, UUID id) {
        try {
            UUID applicationID = UUID.fromString(applicationIdString);
            var result = backboneClient.findUserById(id);
            var profileImageRef = backboneClient.getProfileImageRef(token, applicationID);
            var profileRef = Objects.nonNull(profileImageRef.getBody()) && Objects.nonNull(profileImageRef.getBody().ref()) ?
                    profileImageRef.getBody().ref() : "";
            var businessIds = businessRepository.findIdCollectionByUserId(id);
            return ResponseEntity.ok(getUserMapper.fromBackbone(result, profileRef, businessIds));
        } catch (FeignException e) {
            logger.warn("Error finding user: {}", e.getMessage());
            if (e.status() == HttpStatus.NOT_FOUND.value()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).header("message-error", USER_NOT_FOUND_MESSAGE).build();
            }
            return ResponseEntity.status(e.status()).build();
        }
    }

    @Override
    public ResponseEntity<Void> update(UUID userId, PutUserRequest request) {
        try {
            // Validate request object
            if (Objects.isNull(request)) {
                logger.warn("Update request is null for user: {}", userId);
                return ResponseEntity.status(BAD_REQUEST)
                        .header(MESSAGE_HEADER, "Request body is missing.")
                        .build();
            }

            // Validate user ID
            if (Objects.isNull(userId)) {
                logger.warn("User ID is null");
                return ResponseEntity.status(BAD_REQUEST)
                        .header(MESSAGE_HEADER, "User ID is required.")
                        .build();
            }

            // Validate role IDs if provided
            if (request.roleIds() != null) {
                ResponseEntity<Void> roleValidation = validateRoleIds(request.roleIds());
                if (roleValidation != null) {
                    return roleValidation;
                }
            }

            // Check if user exists before updating
            try {
                var existingUser = backboneClient.findUserById(userId);
                if (existingUser == null) {
                    logger.warn("User not found: {}", userId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .header(MESSAGE_HEADER, USER_NOT_FOUND_MESSAGE)
                            .build();
                }
            } catch (FeignException e) {
                if (e.status() == HttpStatus.NOT_FOUND.value()) {
                    logger.warn("User not found: {}", userId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .header(MESSAGE_HEADER, USER_NOT_FOUND_MESSAGE)
                            .build();
                }
                throw e;
            }

            BackboneUserUpdateRequest backboneRequest = putUserMapper.toBackbone(UUID.fromString(applicationIdString), request);
            logger.info("Updating user: {} with request: {}", userId, backboneRequest);
            return backboneClient.userPartialUpdate(userId, backboneRequest);
        } catch (FeignException e) {
            logger.warn("Feign exception while updating user {}: {}", userId, e.getMessage());
            if (e.status() == HttpStatus.NOT_FOUND.value()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header(MESSAGE_HEADER, USER_NOT_FOUND_MESSAGE)
                        .build();
            } else if (e.status() == BAD_REQUEST.value()) {
                return ResponseEntity.status(BAD_REQUEST)
                        .header(MESSAGE_HEADER, "Invalid request data.")
                        .build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(MESSAGE_HEADER, "Error updating user.")
                    .build();
        } catch (Exception e) {
            logger.error("Unexpected error updating user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(MESSAGE_HEADER, "An unexpected error occurred while updating the user.")
                    .build();
        }
    }

    /**
     * Deletes a user by user ID and application ID using the BackboneClient.
     *
     * @param userId the ID of the user to delete
     * @return a ResponseEntity with appropriate status
     */
    @Override
    public ResponseEntity<Void> deleteUserByUserAndApplication(UUID userId) {
        var applicationId = UUID.fromString(applicationIdString);
        logger.info("Attempting to delete user with ID: {} for application: {}", userId, applicationId);

        try {
            // Call the BackboneClient to delete the user
            ResponseEntity<Void> response = backboneClient.deleteUserByUserIdAndApplicationId(applicationId, userId);

            if (response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
                logger.info("Successfully deleted user with ID: {} for application: {}", userId, applicationId);
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } catch (FeignException e) {
            if (e.status() == HttpStatus.NOT_FOUND.value()) {
                logger.warn("User with ID {} not found for deletion in application: {}", userId, applicationId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            if (e.status() == HttpStatus.FORBIDDEN.value()) {
                logger.warn("Access denied for deleting user with ID: {} in application: {}", userId, applicationId);
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
            if (e.status() == BAD_REQUEST.value()) {
                logger.warn("Invalid request data for deletion in application: {}", applicationId);
                return new ResponseEntity<>(BAD_REQUEST);
            }
            logger.error("Unexpected response status: {} when deleting user with ID: {} for application: {}",
                    e.status(), userId, applicationId);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            logger.warn("Error deleting user with ID: {} for application: {}", userId, applicationId, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
    }

    /**
     * Validates role IDs for edge cases.
     *
     * @param roleIds the collection of role IDs to validate
     * @return ResponseEntity with error details if validation fails, null if validation passes
     */
    private ResponseEntity<Void> validateRoleIds(List<UUID> roleIds) {
        // Check for empty collection (removing all roles)
        if (roleIds.isEmpty()) {
            logger.warn("Attempting to remove all roles from user");
            return ResponseEntity.status(BAD_REQUEST)
                    .header(MESSAGE_HEADER, "Cannot remove all roles from user. At least one role is required.")
                    .build();
        }

        // Check for null role IDs
        if (roleIds.stream().anyMatch(Objects::isNull)) {
            logger.warn("Role collection contains null values");
            return ResponseEntity.status(BAD_REQUEST)
                    .header(MESSAGE_HEADER, "Role collection contains invalid null values.")
                    .build();
        }

        // Check for duplicate role IDs
        if (roleIds.size() != roleIds.stream().distinct().count()) {
            logger.warn("Role collection contains duplicate values");
            return ResponseEntity.status(BAD_REQUEST)
                    .header(MESSAGE_HEADER, "Role collection contains duplicate values.")
                    .build();
        }

        // Check for invalid/empty UUIDs
        for (UUID roleId : roleIds) {
            if (roleId.toString().equals("00000000-0000-0000-0000-000000000000")) {
                logger.warn("Role collection contains invalid UUID: {}", roleId);
                return ResponseEntity.status(BAD_REQUEST)
                        .header(MESSAGE_HEADER, "Role collection contains invalid role ID.")
                        .build();
            }
        }

        return null; // Validation passed
    }

    /// Generates a random four-digit number.
    ///
    /// This method uses [SecureRandom] to generate a random integer between 1000 and 9999 (inclusive), ensuring
    /// that the result is always a four-digit number. This can be used for purposes such as verification codes or temporary PINs.
    /// @return a random four-digit integer between 1000 and 9999
    public int generateFourDigitNumber() {
        SecureRandom random = new SecureRandom();
        return 1000 + random.nextInt(9000); // Generates a number from 1000 to 9999
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
            backboneClient.checkAlias(alias, UUID.fromString(applicationIdString));
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
        return generateFourDigitNumber() +
                "-" + generateFourDigitNumber();
    }
}
