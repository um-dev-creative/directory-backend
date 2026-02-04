package com.umdev.directory.api.v1.service;

import com.umdev.directory.api.v1.to.ProductCreateRequest;
import com.umdev.directory.api.v1.to.ProductCreateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductServiceTest {

    private final ProductService productService = new ProductService() {};

    @Test
    @DisplayName("Create product successfully")
    void createProductSuccessfully() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Example Product",
                "This is an example product description.",
                UUID.randomUUID());
        ResponseEntity<ProductCreateResponse> response = productService.create(request);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Test
    @DisplayName("Create product with null request")
    void createProductWithNullRequest() {
        ResponseEntity<ProductCreateResponse> response = productService.create(null);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }
}
