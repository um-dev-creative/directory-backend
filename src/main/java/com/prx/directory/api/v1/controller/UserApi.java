package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.UserService;
import com.prx.directory.api.v1.to.UserCreateRequest;
import com.prx.directory.api.v1.to.UserCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/// User API interface for handling user-related operations.
@Tag(name="user", description="The user API")
public interface UserApi {

    /// Provides an instance of UserService.
    ///
    /// @return an instance of UserService
    default UserService getService() {
        return new UserService() {};
    }

    /// Handles the creation of a new user.
    ///
    /// @param userCreateRequest the request object containing user creation details
    /// @return a ResponseEntity containing the response of the user creation operation
    @Operation(summary = "Create a new user", description = "Creates a new user in the system with the provided details.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "Conflict: Alias or email already exists")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<UserCreateResponse> createUserPost(@RequestBody UserCreateRequest userCreateRequest) {
        return this.getService().create(userCreateRequest);
    }

}
