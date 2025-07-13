package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.BusinessCreateRequest;
import com.prx.directory.api.v1.to.BusinessCreateResponse;
import com.prx.directory.api.v1.to.BusinessTO;
import com.prx.directory.api.v1.to.LinkBusinessProductResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

/// Service interface for business-related operations.
public interface BusinessService {

    /// Creates a new business.
    ///
    /// @param businessCreateRequest the request object containing business details
    /// @return a ResponseEntity containing the response object and HTTP status
    default ResponseEntity<BusinessCreateResponse> create(@NotNull BusinessCreateRequest businessCreateRequest) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// Finds a business by its ID.
    ///
    /// @param id the UUID of the business
    /// @return a ResponseEntity containing the business transfer object and HTTP status
    default ResponseEntity<BusinessTO> findById(@NotNull UUID id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// Finds a business by its name.
    ///
    /// @param name the name of the business
    /// @return a ResponseEntity containing the business transfer object and HTTP status
    default ResponseEntity<BusinessTO> findByName(@NotNull String name) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// Finds businesses by user ID.
    ///
    /// @param userId the UUID of the user
    /// @param pageable the pagination information
    /// @return a ResponseEntity containing a page of business transfer objects and HTTP status
    default ResponseEntity<Page<BusinessTO>> findByUserId(UUID userId, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    default ResponseEntity<LinkBusinessProductResponse> link(UUID businessId, UUID productId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Deletes a business by its ID and user ID (owner validation required).
     *
     * @param businessId the UUID of the business to delete
     * @param token the UUID of the user attempting the deletion
     * @return a ResponseEntity indicating the result of the delete operation
     */
    default ResponseEntity<Void> deleteBusiness(@NotNull UUID businessId, @NotNull String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

}
