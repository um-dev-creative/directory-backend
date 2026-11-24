package com.umdc.directory.api.v1.service;

import com.umdc.commons.services.cloudflare.exception.ImageStorageException;
import com.umdc.commons.services.cloudflare.exception.ImageUploadException;
import com.umdc.commons.services.cloudflare.exception.ImageValidationException;
import com.umdc.commons.services.cloudflare.r2.client.CloudflareR2StorageClient;
import com.umdc.commons.services.cloudflare.to.ImageReferenceResponse;
import com.umdc.commons.services.cloudflare.to.ImageUploadRequest;
import com.umdc.commons.services.cloudflare.to.ImageUploadResponse;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.support.TestTokens;
import com.umdc.directory.jpa.repository.BusinessRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessProfileImageServiceImpl")
class BusinessProfileImageServiceImplTest {

    @Mock
    private CloudflareR2StorageClient r2;

    @Mock
    private BusinessRepository businessRepository;

    @InjectMocks
    private BusinessProfileImageServiceImpl service;

    private static final byte[] IMAGE_BYTES = new byte[]{1, 2, 3};
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final String OWNER_TOKEN = TestTokens.forUser(OWNER_ID);

    private static BusinessEntity businessOf(UUID businessId, UUID ownerId) {
        BusinessEntity entity = new BusinessEntity();
        entity.setId(businessId);
        if (ownerId != null) {
            entity.setUserFk(ownerId);
        }
        return entity;
    }

    private static final String PUBLIC_URL = "https://cdn.example.com/businesses/photo.jpg";

    // -------------------------------------------------------------------------
    // upload(ImageUploadRequest)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("upload - client objectKey is ignored and a server key is generated")
    void upload_withExplicitKey_ignoresClientKey() throws ImageValidationException, ImageUploadException {
        String key = "other-user/overwrite-me.jpg";
        when(r2.uploadImage(eq(IMAGE_BYTES), argThat(k -> k.startsWith("businesses/") && !k.equals(key)), eq("image/jpeg"))).thenReturn("k");
        when(r2.getPublicUrl(anyString())).thenReturn(PUBLIC_URL);

        ResponseEntity<ImageUploadResponse> response = service.upload(ImageUploadRequest.of(key, IMAGE_BYTES, "image/jpeg"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotEquals(key, response.getBody().objectKey());
        assertTrue(response.getBody().objectKey().startsWith("businesses/"));
        verify(r2, never()).uploadImage(any(), eq(key), anyString());
    }

    @Test
    @DisplayName("upload - null objectKey triggers auto-generated key under businesses/ prefix")
    void upload_nullObjectKey_generatesKeyWithBusinessesPrefix() throws ImageValidationException, ImageUploadException {
        when(r2.uploadImage(eq(IMAGE_BYTES), argThat(k -> k.startsWith("businesses/")), eq("image/jpeg"))).thenReturn("generated");
        when(r2.getPublicUrl(anyString())).thenReturn(PUBLIC_URL);

        ImageUploadRequest request = ImageUploadRequest.of(IMAGE_BYTES, "image/jpeg");
        ResponseEntity<ImageUploadResponse> response = service.upload(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().objectKey().startsWith("businesses/"));
        assertTrue(response.getBody().objectKey().endsWith(".jpg"));
    }

    @ParameterizedTest(name = "contentType={0} → ext={1}")
    @CsvSource({
        "image/png,  .png",
        "image/webp, .webp",
        "image/gif,  .gif",
        "image/jpeg, .jpg",
        "image/bmp,  .jpg"   // unknown → .jpg
    })
    @DisplayName("upload - generated key extension matches content type")
    void upload_nullObjectKey_extensionMatchesContentType(String contentType, String expectedExt)
            throws ImageValidationException, ImageUploadException {
        String trimmedExt = expectedExt.trim();
        when(r2.uploadImage(eq(IMAGE_BYTES), argThat(k -> k.endsWith(trimmedExt)), eq(contentType))).thenReturn("key");
        when(r2.getPublicUrl(anyString())).thenReturn(PUBLIC_URL);

        ImageUploadRequest request = ImageUploadRequest.of(IMAGE_BYTES, contentType);
        ResponseEntity<ImageUploadResponse> response = service.upload(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().objectKey().endsWith(trimmedExt));
    }

    // -------------------------------------------------------------------------
    // uploadForBusiness(UUID, byte[], String)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("uploadForBusiness - business found, uploads and persists URL")
    void uploadForBusiness_found_uploadsAndPersistsUrl() throws ImageValidationException, ImageUploadException {
        UUID businessId = UUID.randomUUID();
        BusinessEntity entity = businessOf(businessId, OWNER_ID);

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(entity));
        when(r2.uploadImage(eq(IMAGE_BYTES), argThat(k -> k.startsWith("businesses/" + businessId)), eq("image/png"))).thenReturn("key");
        when(r2.getPublicUrl(anyString())).thenReturn(PUBLIC_URL);
        when(businessRepository.save(entity)).thenReturn(entity);

        ResponseEntity<ImageUploadResponse> response = service.uploadForBusiness(businessId, IMAGE_BYTES, "image/png", OWNER_TOKEN);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().objectKey().startsWith("businesses/" + businessId + "/"));
        assertTrue(response.getBody().objectKey().endsWith(".png"));
        assertEquals(PUBLIC_URL, response.getBody().publicUrl());
        assertEquals(PUBLIC_URL, entity.getProfileImageRef());
        verify(businessRepository).save(entity);
    }

    @Test
    @DisplayName("uploadForBusiness - business not found returns 404")
    void uploadForBusiness_notFound_returns404() throws ImageValidationException, ImageUploadException {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.empty());

        ResponseEntity<ImageUploadResponse> response = service.uploadForBusiness(businessId, IMAGE_BYTES, "image/jpeg", OWNER_TOKEN);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(r2);
        verify(businessRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadForBusiness - key is scoped to businessId/uuid.ext")
    void uploadForBusiness_keyIncludesBusinessId() throws ImageValidationException, ImageUploadException {
        UUID businessId = UUID.randomUUID();
        BusinessEntity entity = businessOf(businessId, OWNER_ID);

        when(businessRepository.findById(businessId)).thenReturn(Optional.of(entity));
        when(r2.uploadImage(any(), anyString(), anyString())).thenReturn("key");
        when(r2.getPublicUrl(anyString())).thenReturn(PUBLIC_URL);
        when(businessRepository.save(any())).thenReturn(entity);

        service.uploadForBusiness(businessId, IMAGE_BYTES, "image/webp", OWNER_TOKEN);

        verify(r2).uploadImage(eq(IMAGE_BYTES),
                argThat(k -> k.startsWith("businesses/" + businessId + "/") && k.endsWith(".webp")),
                eq("image/webp"));
    }

    @Test
    @DisplayName("uploadForBusiness - another user (not the owner) gets 403 and nothing is stored")
    void uploadForBusiness_notOwner_returns403() throws ImageValidationException, ImageUploadException {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(businessOf(businessId, UUID.randomUUID())));

        ResponseEntity<ImageUploadResponse> response = service.uploadForBusiness(businessId, IMAGE_BYTES, "image/png", OWNER_TOKEN);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(r2);
        verify(businessRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadForBusiness - business without owner gets 403")
    void uploadForBusiness_noOwner_returns403() throws ImageValidationException, ImageUploadException {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(businessOf(businessId, null)));

        assertEquals(HttpStatus.FORBIDDEN,
                service.uploadForBusiness(businessId, IMAGE_BYTES, "image/png", OWNER_TOKEN).getStatusCode());
        verifyNoInteractions(r2);
    }

    @Test
    @DisplayName("uploadForBusiness - missing or invalid token gets 401 without touching the repository")
    void uploadForBusiness_noToken_returns401() throws ImageValidationException, ImageUploadException {
        UUID businessId = UUID.randomUUID();

        assertEquals(HttpStatus.UNAUTHORIZED, service.uploadForBusiness(businessId, IMAGE_BYTES, "image/png", null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.uploadForBusiness(businessId, IMAGE_BYTES, "image/png", "garbage").getStatusCode());
        verifyNoInteractions(r2, businessRepository);
    }

    // -------------------------------------------------------------------------
    // getReference(String)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getReference - returns public URL wrapped in ImageReferenceResponse")
    void getReference_returnsPublicUrl() throws ImageStorageException {
        String objectKey = "businesses/abc.jpg";
        when(r2.getPublicUrl(objectKey)).thenReturn(PUBLIC_URL);

        ResponseEntity<ImageReferenceResponse> response = service.getReference(objectKey);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(objectKey, response.getBody().objectKey());
        assertEquals(PUBLIC_URL, response.getBody().publicUrl());
        assertNull(response.getBody().contentType());
    }
}
