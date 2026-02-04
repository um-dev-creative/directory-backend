package com.umdev.directory.api.v1.controller;

import com.umdev.directory.api.v1.service.ProfileImageService;
import com.umdev.directory.api.v1.to.PostProfileImageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ProfileImageController implements the ProfileImageApi interface.
 */
@RestController
@RequestMapping("/api/v1/profile/image")
public class ProfileImageController implements ProfileImageApi {

    private final ProfileImageService profileImageService;

    public ProfileImageController(ProfileImageService profileImageService) {
        this.profileImageService = profileImageService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<PostProfileImageResponse> saveProfileImage(String token, byte[] imageData) {
        return profileImageService.save(token, imageData);
    }
}

