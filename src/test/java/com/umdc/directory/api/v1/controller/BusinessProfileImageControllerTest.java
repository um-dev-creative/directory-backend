package com.umdc.directory.api.v1.controller;

import com.umdc.commons.services.cloudflare.exception.ImageStorageException;
import com.umdc.commons.services.cloudflare.exception.ImageUploadException;
import com.umdc.commons.services.cloudflare.exception.ImageValidationException;
import com.umdc.commons.services.cloudflare.to.ImageReferenceResponse;
import com.umdc.commons.services.cloudflare.to.ImageUploadResponse;
import com.umdc.directory.api.v1.service.BusinessProfileImageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessProfileImageController")
class BusinessProfileImageControllerTest {

    @Mock
    private BusinessProfileImageService imageService;

    @InjectMocks
    private BusinessProfileImageController controller;

    private static final byte[] IMAGE_BYTES = new byte[]{1, 2, 3};
    private static final String TOKEN = "test-token";

    // -------------------------------------------------------------------------
    // getService()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getService - returns the injected BusinessProfileImageService")
    void getService_returnsInjectedService() {
        assertSame(imageService, controller.getService());
    }

    // -------------------------------------------------------------------------
    // upload(token, objectKey, image, contentType)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("upload - with explicit objectKey delegates to service.upload")
    void upload_withExplicitKey_delegatesToService() throws ImageValidationException, ImageUploadException {
        String key = "businesses/custom.jpg";
        ImageUploadResponse body = new ImageUploadResponse(key, "https://cdn/photo.jpg", "image/jpeg", IMAGE_BYTES.length);
        when(imageService.upload(argThat(r -> key.equals(r.objectKey())))).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(body));

        ResponseEntity<ImageUploadResponse> response = controller.upload(TOKEN, key, IMAGE_BYTES, "image/jpeg");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(key, response.getBody().objectKey());
    }

    @Test
    @DisplayName("upload - null objectKey builds request with auto-generated key")
    void upload_nullObjectKey_buildsRequestWithNullKey() throws ImageValidationException, ImageUploadException {
        ImageUploadResponse body = new ImageUploadResponse("businesses/gen.jpg", "https://cdn/gen.jpg", "image/jpeg", IMAGE_BYTES.length);
        when(imageService.upload(argThat(r -> r.objectKey() == null))).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(body));

        ResponseEntity<ImageUploadResponse> response = controller.upload(TOKEN, null, IMAGE_BYTES, "image/jpeg");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // getReference(token, objectKey)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getReference - delegates to service.getReference and returns result")
    void getReference_delegatesToService() throws ImageStorageException {
        String key = "businesses/abc.jpg";
        ImageReferenceResponse body = new ImageReferenceResponse(key, "https://cdn/abc.jpg", null);
        when(imageService.getReference(key)).thenReturn(ResponseEntity.ok(body));

        ResponseEntity<ImageReferenceResponse> response = controller.getReference(TOKEN, key);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(key, response.getBody().objectKey());
        verify(imageService).getReference(key);
    }

    // -------------------------------------------------------------------------
    // uploadForBusiness(token, businessId, image, contentType)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("uploadForBusiness - delegates to service with businessId and resolved contentType")
    void uploadForBusiness_delegatesToService() throws ImageValidationException, ImageUploadException {
        UUID businessId = UUID.randomUUID();
        ImageUploadResponse body = new ImageUploadResponse("businesses/" + businessId + "/x.png",
                "https://cdn/photo.png", "image/png", IMAGE_BYTES.length);
        when(imageService.uploadForBusiness(businessId, IMAGE_BYTES, "image/png"))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(body));

        ResponseEntity<ImageUploadResponse> response = controller.uploadForBusiness(TOKEN, businessId, IMAGE_BYTES, "image/png");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("image/png", response.getBody().contentType());
        verify(imageService).uploadForBusiness(businessId, IMAGE_BYTES, "image/png");
    }

    @Test
    @DisplayName("uploadForBusiness - null contentType defaults to image/jpeg")
    void uploadForBusiness_nullContentType_defaultsToJpeg() throws ImageValidationException, ImageUploadException {
        UUID businessId = UUID.randomUUID();
        ImageUploadResponse body = new ImageUploadResponse("businesses/" + businessId + "/x.jpg",
                "https://cdn/photo.jpg", "image/jpeg", IMAGE_BYTES.length);
        when(imageService.uploadForBusiness(businessId, IMAGE_BYTES, "image/jpeg"))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(body));

        ResponseEntity<ImageUploadResponse> response = controller.uploadForBusiness(TOKEN, businessId, IMAGE_BYTES, null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(imageService).uploadForBusiness(businessId, IMAGE_BYTES, "image/jpeg");
    }

    @Test
    @DisplayName("uploadForBusiness - business not found propagates 404 from service")
    void uploadForBusiness_businessNotFound_returns404() throws ImageValidationException, ImageUploadException {
        UUID businessId = UUID.randomUUID();
        when(imageService.uploadForBusiness(businessId, IMAGE_BYTES, "image/jpeg"))
                .thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

        ResponseEntity<ImageUploadResponse> response = controller.uploadForBusiness(TOKEN, businessId, IMAGE_BYTES, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
