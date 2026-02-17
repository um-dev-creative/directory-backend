package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.PostProfileImageResponse;
import com.prx.directory.client.backbone.BackboneClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ProfileImageServiceImpl - unit tests for profile image save behavior")
class ProfileImageServiceImplTest {

    BackboneClient backboneClient;
    ProfileImageServiceImpl service;

    @BeforeEach
    void setup() {
        backboneClient = mock(BackboneClient.class);
        service = new ProfileImageServiceImpl(backboneClient);
        // set applicationIdString via Reflection
        ReflectionTestUtils.setField(service, "applicationIdString", UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("save returns OK and body when backbone returns OK")
    void save_returnsOk_whenBackboneOk() {
        PostProfileImageResponse body = new PostProfileImageResponse("ref123");
        when(backboneClient.saveProfilePhoto(any(), any(UUID.class), any())).thenReturn(ResponseEntity.ok(body));

        ResponseEntity<PostProfileImageResponse> resp = service.save("token", new byte[0]);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(body, resp.getBody());
    }

    @Test
    @DisplayName("save returns backend response when backbone returns non-OK")
    void save_returnsBackendResponse_whenNotOk() {
        when(backboneClient.saveProfilePhoto(any(), any(UUID.class), any())).thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        ResponseEntity<PostProfileImageResponse> resp = service.save("token", new byte[0]);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }
}

