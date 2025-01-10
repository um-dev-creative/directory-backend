package com.prx.directory.api.v1.service;

import com.prx.commons.constants.types.MessageType;
import com.prx.commons.exception.StandardException;
import com.prx.directory.api.v1.to.UserCreateRequest;
import com.prx.directory.api.v1.to.UserCreateResponse;
import com.prx.directory.mapper.UserCreateMapper;
import com.prx.security.client.BackboneClient;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.UUID;

import static com.prx.directory.constant.DirectoryAppConstants.MESSAGE_ERROR_HEADER;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/// Service implementation for user-related operations.
@Service
public class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String USERNAME_ALREADY_EXISTS = "Username {} already exists";
    private static final String EMAIL_ALREADY_EXISTS = "Email already exists";
    private static final String ERROR_CREATING_USER = "Error creating user";
    private static final int MAX_ALIAS_LENGTH = 12;
    private static final int MAX_ALIAS_TRY = 5;

    @Value("${prx.directory.application-id}")
    private String applicationId;
    @Value("${prx.directory.role-id}")
    private String initialRoleId;

    private final BackboneClient backboneClient;
    private final UserCreateMapper userCreateMapper;

    /// Constructs a new UserServiceImpl with the specified BackboneClient and UserCreateMapper.
    ///
    /// @param backboneClient   the client used to communicate with the backend
    /// @param userCreateMapper the mapper used to convert between request/response objects and backend objects
    public UserServiceImpl(BackboneClient backboneClient, UserCreateMapper userCreateMapper) {
        this.backboneClient = backboneClient;
        this.userCreateMapper = userCreateMapper;
    }

    /// Creates a new user based on the provided UserCreateRequest.
    ///
    /// @param userCreateRequest the request object containing user details
    /// @return a ResponseEntity containing the UserCreateResponse
    @Override
    public ResponseEntity<UserCreateResponse> create(UserCreateRequest userCreateRequest) {
        UUID applicationID = UUID.fromString(applicationId);
        try {
            if (Objects.isNull(userCreateRequest)) {
                return ResponseEntity.status(BAD_REQUEST).header(MESSAGE_ERROR_HEADER, "Content null invalid").build();
            }
            backboneClient.checkEmail(userCreateRequest.email(), applicationID);
            var backboneUserCreateRequest = userCreateMapper.toBackbone(userCreateRequest,
                    applicationID, UUID.fromString(initialRoleId), generateAlias(userCreateRequest, true, 1));
            var userCreateResponse = userCreateMapper.fromBackbone(backboneClient.post(backboneUserCreateRequest));
            return ResponseEntity.status(HttpStatus.CREATED).body(userCreateResponse);
        } catch (FeignException e) {
            LOGGER.warn("Error creating user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).header(MESSAGE_ERROR_HEADER, EMAIL_ALREADY_EXISTS).build();
        } catch (StandardException e) {
            LOGGER.warn("Standard Error creating user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).header(MESSAGE_ERROR_HEADER, ERROR_CREATING_USER).build();
        }
    }

    private String generateAlias(UserCreateRequest userCreateRequest, boolean afterFirstTime, int time) {
        SecureRandom random = new SecureRandom();
        String alias;
        StringBuilder aliasTemp = new StringBuilder(userCreateRequest.firstname().substring(0, 1).toLowerCase()
                .concat(userCreateRequest.lastname().toLowerCase()));

        random.nextBytes(new byte[20]);
        if (afterFirstTime) {
            if(aliasTemp.length() >= MAX_ALIAS_LENGTH) {
                aliasTemp.delete((MAX_ALIAS_LENGTH-5), MAX_ALIAS_LENGTH);
            }
            aliasTemp.append(random.nextInt());
        }
        alias = fixAlias(aliasTemp);
        try {
            backboneClient.checkAlias(alias, UUID.fromString(applicationId));
            return alias;
        } catch (FeignException e) {
            LOGGER.warn(USERNAME_ALREADY_EXISTS, alias);
        }

        if(time <= MAX_ALIAS_TRY) {
            time+=1;
            return generateAlias(userCreateRequest, false, time);
        }
        throw new StandardException("Alias generation failed", MessageType.DEFAULT_MESSAGE);
    }

    private String fixAlias(StringBuilder alias) {
        if(alias.length() >= MAX_ALIAS_LENGTH) {
            return alias.substring(0,MAX_ALIAS_LENGTH-1);
        }
        return alias.toString();
    }

}
