package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.UserService;
import com.prx.directory.api.v1.to.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
        @ApiResponse(responseCode = "201", description = "User created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserCreateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "409", description = "Conflict: Alias or email already exists", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<UserCreateResponse> createUserPost(@RequestBody UserCreateRequest userCreateRequest) {
        return this.getService().create(userCreateRequest);
    }

    /// Handles the retrieval of a user by ID.
    ///
    /// @param id the ID of the user to retrieve
    /// @return a ResponseEntity containing the response of the user retrieval operation
    /// @see GetUserResponse
    /// @see ResponseEntity
    /// @see UUID
    /// @see UserService
    /// @see UserApi
    /// @see UserCreateRequest
    /// @see UserCreateResponse
    @Operation(summary = "Get a user by ID", description = "Retrieves a user in the system with the provided ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = GetUserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<GetUserResponse> userGet(@NotNull @PathVariable UUID id) {
        return this.getService().findUser(id);
    }

    /// Updates a user by ID.
    ///
    /// @param userId the ID of the user to update
    /// @param request the patch user request
    /// @return the patch user response
    @Operation(summary = "Update a user by ID", description = "Updates a user in the system with the provided ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "User updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Void.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @PutMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<Void> update(
            @PathVariable("userId") UUID userId,
            @RequestBody PutUserRequest request) {
        return this.getService().update(userId, request);
    }

    /// Deletes a user by user ID and application ID.
    ///
    /// @param userId the ID of the user to delete
    /// @return a ResponseEntity with appropriate status
    @Operation(summary = "Delete a user by ID", description = "Deletes a user from the system with the provided user ID. Requires admin or appropriate authorization.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden: Access denied", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @DeleteMapping(value = "/{userId}")
    default ResponseEntity<Void> deleteUserByUserId(@NotNull @PathVariable UUID userId) {
        return this.getService().deleteUserByUserAndApplication(userId);
    }

}
