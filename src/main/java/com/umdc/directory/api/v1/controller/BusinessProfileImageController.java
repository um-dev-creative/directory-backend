package com.umdc.directory.api.v1.controller;

import com.umdc.commons.services.cloudflare.controller.ImageApi;
import com.umdc.commons.services.cloudflare.exception.ImageStorageException;
import com.umdc.commons.services.cloudflare.exception.ImageUploadException;
import com.umdc.commons.services.cloudflare.exception.ImageValidationException;
import com.umdc.commons.services.cloudflare.service.ImageService;
import com.umdc.commons.services.cloudflare.to.ImageReferenceResponse;
import com.umdc.commons.services.cloudflare.to.ImageUploadRequest;
import com.umdc.commons.services.cloudflare.to.ImageUploadResponse;
import com.umdc.directory.api.v1.service.BusinessProfileImageService;
import com.umdc.directory.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * BusinessProfileImageController exposes image upload and retrieval operations
 * for business profiles at {@code /api/v1/businesses/images}.
 */
@RestController
@RequestMapping("/api/v1/businesses/images")
public class BusinessProfileImageController implements ImageApi {

    private final BusinessProfileImageService imageService;

    public BusinessProfileImageController(BusinessProfileImageService imageService) {
        this.imageService = imageService;
    }

    /** {@inheritDoc} */
    @Override
    public ImageService getService() {
        return imageService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Requires a valid session token (401 otherwise). The client-supplied {@code objectKey} is
     * ignored: the service always generates the key under the {@code businesses/} prefix.
     */
    @Override
    public ResponseEntity<ImageUploadResponse> upload(
            String token, String objectKey, byte[] image, String contentType)
            throws ImageValidationException, ImageUploadException {
        if (JwtUtil.getUidFromToken(token) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return imageService.upload(ImageUploadRequest.of(image, contentType));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ImageReferenceResponse> getReference(String token, String objectKey)
            throws ImageStorageException {
        return imageService.getReference(objectKey);
    }

    /**
     * Uploads a profile image for the given business, stores it in Cloudflare R2, and
     * persists the public URL in {@code business.profile_image_ref}.
     *
     * <p>POST /api/v1/businesses/images/{businessId}
     *
     * @param token       session token
     * @param businessId  UUID of the business to update
     * @param image       raw image bytes
     * @param contentType MIME type (defaults to {@code image/jpeg} when omitted)
     * @return 201, 401 without a valid token, 403 when the caller does not own the business, 404 when missing
     */
    @PostMapping(value = "/{businessId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadForBusiness(
            @RequestHeader(SESSION_TOKEN_KEY) String token,
            @PathVariable UUID businessId,
            @RequestPart byte[] image,
            @RequestPart(required = false) String contentType)
            throws ImageValidationException, ImageUploadException {
        String resolvedContentType = contentType != null ? contentType : "image/jpeg";
        return imageService.uploadForBusiness(businessId, image, resolvedContentType, token);
    }
}
