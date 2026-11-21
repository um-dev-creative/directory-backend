package com.umdc.directory.api.v1.service;

import com.umdc.commons.services.cloudflare.exception.ImageStorageException;
import com.umdc.commons.services.cloudflare.exception.ImageUploadException;
import com.umdc.commons.services.cloudflare.exception.ImageValidationException;
import com.umdc.commons.services.cloudflare.r2.client.CloudflareR2StorageClient;
import com.umdc.commons.services.cloudflare.to.ImageReferenceResponse;
import com.umdc.commons.services.cloudflare.to.ImageUploadRequest;
import com.umdc.commons.services.cloudflare.to.ImageUploadResponse;
import com.umdc.directory.jpa.repository.BusinessRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * BusinessProfileImageServiceImpl handles upload and retrieval of business profile images
 * stored in Cloudflare R2.
 */
@Service
public class BusinessProfileImageServiceImpl implements BusinessProfileImageService {

    private final CloudflareR2StorageClient r2;
    private final BusinessRepository businessRepository;

    public BusinessProfileImageServiceImpl(CloudflareR2StorageClient r2, BusinessRepository businessRepository) {
        this.r2 = r2;
        this.businessRepository = businessRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generic upload (no business context). When {@code request.objectKey()} is {@code null}
     * a key is auto-generated under the {@code businesses/} prefix.
     * Use {@link #uploadForBusiness} to associate the image with a specific business record.
     */
    @Override
    public ResponseEntity<ImageUploadResponse> upload(ImageUploadRequest request)
            throws ImageValidationException, ImageUploadException {
        String key = request.objectKey() != null ? request.objectKey() : generateKey(null, request.contentType());
        r2.uploadImage(request.data(), key, request.contentType());
        String url = r2.getPublicUrl(key);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ImageUploadResponse(key, url, request.contentType(), request.data().length));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates a key scoped to the business ({@code businesses/{businessId}/{uuid}.{ext}}),
     * uploads the image to Cloudflare R2, and persists the public URL in
     * {@code business.profile_image_ref}.
     */
    @Override
    public ResponseEntity<ImageUploadResponse> uploadForBusiness(UUID businessId, byte[] image, String contentType)
            throws ImageValidationException, ImageUploadException {
        var businessOpt = businessRepository.findById(businessId);
        if (businessOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String key = generateKey(businessId, contentType);
        r2.uploadImage(image, key, contentType);
        String url = r2.getPublicUrl(key);

        var business = businessOpt.get();
        business.setProfileImageRef(url);
        businessRepository.save(business);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ImageUploadResponse(key, url, contentType, image.length));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the public URL for the given object key without downloading the bytes.
     */
    @Override
    public ResponseEntity<ImageReferenceResponse> getReference(String objectKey) throws ImageStorageException {
        String url = r2.getPublicUrl(objectKey);
        return ResponseEntity.ok(new ImageReferenceResponse(objectKey, url, null));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Generates a unique storage key.
     * When {@code businessId} is provided the key is scoped as
     * {@code businesses/{businessId}/{uuid}.{ext}}, otherwise {@code businesses/{uuid}.{ext}}.
     */
    private String generateKey(UUID businessId, String contentType) {
        String ext = switch (contentType) {
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            case "image/gif"  -> "gif";
            default           -> "jpg";
        };
        String prefix = businessId != null
                ? "businesses/" + businessId + "/"
                : "businesses/";
        return prefix + UUID.randomUUID() + "." + ext;
    }
}
