package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.LinkBusinessProductRequest;
import com.umdc.directory.api.v1.to.LinkBusinessProductResponse;
import com.umdc.directory.api.v1.to.ProductCreateRequest;
import com.umdc.directory.api.v1.to.ProductCreateResponse;
import com.umdc.directory.api.v1.to.ProductListResponse;
import com.umdc.directory.api.v1.to.ProductUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

/**
 * Interface defining operations related to product management.
 *
 * The ProductService interface declares methods for creating products,
 * linking products to businesses, and retrieving a paginated list of products
 * associated with a specific business. These methods provide default
 * implementations that return a "Not Implemented" HTTP status, and can
 * be overridden by implementing classes to provide specific functionality.
 */
public interface ProductService {

    default ResponseEntity<ProductCreateResponse> create(ProductCreateRequest productCreateRequest, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    default ResponseEntity<LinkBusinessProductResponse> linkProductToBusiness(LinkBusinessProductRequest businessProductLinkCreateRequest, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    default ResponseEntity<ProductListResponse> findByBusinessId(UUID businessId, Integer page, Integer perPage, Boolean active) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    default ResponseEntity<ProductListResponse> findByBusinessId(UUID businessId, Integer page, Integer perPage, Boolean active,
                                                                 Boolean includeInactive, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// Updates a product: `replace` false is PATCH (only the sent fields), true is PUT (full replacement).
    default ResponseEntity<ProductCreateResponse> update(UUID productId, ProductUpdateRequest request, String token, boolean replace) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// Soft delete: the product becomes inactive.
    default ResponseEntity<Void> deactivate(UUID productId, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
