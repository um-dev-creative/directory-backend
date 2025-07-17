package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.PostProfileImageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * ProfileImageService interface defines the service operations for profile images.
 */
public interface ProfileImageService {

    /**
     * Saves a profile image associated with the provided token.
     *
     * @param token the authentication token associated with the profile
     * @param imageData the byte array containing the image data to be saved
     * @return a ResponseEntity containing a PostProfileImageResponse with the reference to the saved image
     */
    default ResponseEntity<PostProfileImageResponse> save(String token, byte[] imageData) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
