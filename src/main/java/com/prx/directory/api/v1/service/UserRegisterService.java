package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.ConfirmCodeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/// Service interface for user registration-related operations.
public interface UserRegisterService {

    /// Confirms the verification code for the specified user.
    /// @param sessionTokenBkd The Backbone session token
    /// @param confirmCodeRequest the request object containing the user ID and verification code
    /// @return a ResponseEntity containing the response object and HTTP status
    default ResponseEntity<Void> confirmCode(String sessionTokenBkd, ConfirmCodeRequest confirmCodeRequest) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

}
