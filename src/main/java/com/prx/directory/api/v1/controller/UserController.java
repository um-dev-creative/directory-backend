package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.UserService;
import com.prx.directory.api.v1.to.UseGetResponse;
import com.prx.directory.api.v1.to.UserCreateRequest;
import com.prx.directory.api.v1.to.UserCreateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/// REST controller for user-related operations.
@RestController
@RequestMapping("/api/v1/users")
public class UserController implements UserApi {

    private final UserService userService;

    /// Constructs a new UserController with the specified UserService.
    ///
    /// @param userService the service used to handle user-related operations
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /// Handles the HTTP POST request to create a new user.
    ///
    /// @param userCreateRequest the request object containing user details
    /// @return a ResponseEntity containing the UserCreateResponse
    @Override
    public ResponseEntity<UserCreateResponse> createUserPost(UserCreateRequest userCreateRequest) {
        return userService.create(userCreateRequest);
    }

    /// Handles the HTTP GET request to retrieve a user by ID.
    ///
    /// @param id the ID of the user to retrieve
    /// @return a ResponseEntity containing the UseGetResponse
    @Override
    public ResponseEntity<UseGetResponse> userGet(UUID id) {
        return userService.findUser(id);
    }

}
