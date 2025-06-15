package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.UseGetResponse;
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
    /// @see UseGetResponse
    /// @see ResponseEntity
    /// @see HttpStatus
    /// @see UUID
    default ResponseEntity<UseGetResponse> findUser(UUID id) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

}
