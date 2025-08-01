package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.ProfileImageService;
import com.prx.directory.api.v1.to.PostProfileImageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;

import static com.prx.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * ProfileImageApi interface defines the API operations for managing profile images.
 */
public interface ProfileImageApi {

    default ProfileImageService getService() {
        return new ProfileImageService() {
        };
    }


    /**
     * Saves a profile image for a user session.
     *
     * @param token     the session token used to identify the user, must not be null or empty
     * @param imageData the byte array representing the profile image to be saved
     * @return a string representing the outcome of the save operation or a confirmation message
     */
    @Operation(summary = "Save profile image")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile image saved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    default ResponseEntity<PostProfileImageResponse> saveProfileImage(@Parameter(description = "Token session", required = true) @RequestHeader(SESSION_TOKEN_KEY) String token, @RequestPart byte[] imageData) {
        return this.getService().save(token, imageData);
    }
}
