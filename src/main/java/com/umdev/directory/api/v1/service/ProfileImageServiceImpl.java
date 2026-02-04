package com.umdev.directory.api.v1.service;

import com.umdev.directory.api.v1.to.PostProfileImageResponse;
import com.umdev.directory.client.backbone.BackboneClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * ProfileImageServiceImpl implements the ProfileImageService interface.
 */
@Service
public class ProfileImageServiceImpl implements ProfileImageService {

    @Value("${prx.directory.application-id}")
    private String applicationIdString;

    private final BackboneClient backboneClient;

    @Autowired
    public ProfileImageServiceImpl(BackboneClient backboneClient) {
        this.backboneClient = backboneClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<PostProfileImageResponse> save(String token, byte[] imageData) {
        var response = backboneClient.saveProfilePhoto(token, UUID.fromString(applicationIdString), imageData);
        if(HttpStatus.OK.equals(response.getStatusCode())) {
            var postProfileImageResponse = response.getBody();
            return ResponseEntity.ok(postProfileImageResponse);
        }
        return response;
    }
}
