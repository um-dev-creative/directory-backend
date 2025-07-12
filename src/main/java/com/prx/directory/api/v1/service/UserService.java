package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.GetUserResponse;
import com.prx.directory.api.v1.to.PutUserRequest;
import com.prx.directory.api.v1.to.UserCreateRequest;
import com.prx.directory.api.v1.to.UserCreateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

/// Service interface for user-related operations.
public interface UserService {


    /// Creates a new user.
    ///
    /// @param userCreateRequest the request object containing user details
    /// @return a ResponseEntity containing the response object and HTTP status
    /// @see UserCreateRequest
    /// @see UserCreateResponse
    /// @see ResponseEntity
    /// @see HttpStatus
    default ResponseEntity<UserCreateResponse> create(UserCreateRequest userCreateRequest) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /// Finds a user by ID.
    ///
    /// @param id the ID of the user to find
    /// @return a ResponseEntity containing the response object and HTTP status
    /// @see GetUserResponse
    /// @see ResponseEntity
    /// @see HttpStatus
    /// @see UUID
    default ResponseEntity<GetUserResponse> findUser(UUID id) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /// Updates a user by ID.
    ///
    /// @param userId the ID of the user to update
    /// @param request the patch user request
    /// @return a ResponseEntity containing the patch user response
    default ResponseEntity<Void> update(UUID userId, PutUserRequest request) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /// Deletes a user by user ID and application ID.
    ///
    /// @param userId the ID of the user to delete
    /// @return a ResponseEntity with appropriate status
    /// @see ResponseEntity
    /// @see UUID
    default ResponseEntity<Void> deleteUserByUserAndApplication(UUID userId) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

}
