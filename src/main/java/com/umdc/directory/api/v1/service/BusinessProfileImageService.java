package com.umdc.directory.api.v1.service;

import com.umdc.commons.services.cloudflare.exception.ImageUploadException;
import com.umdc.commons.services.cloudflare.exception.ImageValidationException;
import com.umdc.commons.services.cloudflare.service.ImageService;
import com.umdc.commons.services.cloudflare.to.ImageUploadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

/**
 * BusinessProfileImageService defines the storage contract for business profile images.
 * Extends {@link ImageService} so implementations are also valid Spring beans for the
 * {@code ImageApi} default-method delegation.
 */
public interface BusinessProfileImageService extends ImageService {

    /**
     * Uploads an image for the given business, stores it in Cloudflare R2, and persists
     * the resulting public URL in {@code business.profile_image_ref}.
     *
     * @param businessId  UUID of the business to associate the image with
     * @param image       raw image bytes
     * @param contentType MIME type of the image (e.g. {@code image/jpeg})
     * @param token       session token of the caller, who must own the business
     * @return 201 Created with {@link ImageUploadResponse}, 401 without a valid token, 404 if the business
     *         is not found, or 403 if the caller is not the owner
     */
    default ResponseEntity<ImageUploadResponse> uploadForBusiness(UUID businessId, byte[] image, String contentType, String token)
            throws ImageValidationException, ImageUploadException {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
