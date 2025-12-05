package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.CategoryCreateRequest;
import com.prx.directory.api.v1.to.CategoryCreateResponse;
import com.prx.directory.api.v1.to.CategoryGetResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collection;
import java.util.UUID;

// Service interface for category-related operations.
public interface CategoryService {

    // Retrieves a category by its ID.
    //
    // @param categoryId the ID of the category to retrieve
    // @return a ResponseEntity containing the response object and HTTP status
    // @see CategoryGetResponse
    default ResponseEntity<CategoryGetResponse> find(UUID categoryId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    // Retrieves all categories that have a parent category with the specified ID.
    //
    // @return a ResponseEntity containing the response object and HTTP status
    // @see CategoryGetResponse
    default ResponseEntity<Collection<CategoryGetResponse>> findByParentId(UUID parentId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    // Retrieves all categories.
    //
    // @return a ResponseEntity containing the response object and HTTP status
    // @see CategoryGetResponse
    default ResponseEntity<Collection<CategoryGetResponse>> findAll() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Creates a new category.
     * @param request the create request
     * @return ResponseEntity with CategoryCreateResponse or error status
     */
    default ResponseEntity<CategoryCreateResponse> create(CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
