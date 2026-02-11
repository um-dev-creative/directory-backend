package com.prx.directory.client.backbone;

import com.prx.directory.client.backbone.to.BackboneProfileImageRefResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackboneClientTest {

    @Mock
    private BackboneClient backboneClient;

    @Test
    @DisplayName("BackboneClient: getProfileImageRef returns expected response")
    void testGetProfileImageRef() {
        UUID applicationId = UUID.randomUUID();
        ResponseEntity<BackboneProfileImageRefResponse> expectedReference = ResponseEntity.ok(new BackboneProfileImageRefResponse("imageRef123"));
        String token = "token";

        BackboneClient mockClient = mock(BackboneClient.class);
        when(mockClient.getProfileImageRef(token, applicationId)).thenReturn(expectedReference);

        var result = mockClient.getProfileImageRef(token, applicationId);

        assertEquals(expectedReference, result);
    }
}
