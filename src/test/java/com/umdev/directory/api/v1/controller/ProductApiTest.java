package com.umdev.directory.api.v1.controller;

import com.umdev.directory.api.v1.service.ProductService;
import com.umdev.directory.api.v1.to.LinkBusinessProductRequest;
import com.umdev.directory.api.v1.to.LinkBusinessProductResponse;
import com.umdev.directory.api.v1.to.ProductCreateRequest;
import com.umdev.directory.api.v1.to.ProductCreateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class ProductApiTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductApi productApi = new ProductApi() {};

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("createProduct should return NOT_IMPLEMENTED status with valid request")
    void createProductShouldReturnCreatedStatusWithValidRequest() {
        ProductCreateRequest request = new ProductCreateRequest(
                "product1",
                "description",
                UUID.randomUUID()
        );
        ProductCreateResponse response = new ProductCreateResponse(
                UUID.randomUUID(),
                "product1",
                "description",
                LocalDateTime.now(),
                LocalDateTime.now(),
                UUID.randomUUID()
        );
        when(productService.create(any(ProductCreateRequest.class))).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(response));

        ResponseEntity<ProductCreateResponse> result = productApi.createProduct(request);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, result.getStatusCode());
    }

    @Test
    @DisplayName("linkProductToBusiness should return NOT_IMPLEMENTED status with valid request")
    void linkProductToBusinessShouldReturnOkStatusWithValidRequest() {
        LinkBusinessProductRequest request = new LinkBusinessProductRequest(
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        LinkBusinessProductResponse response = new LinkBusinessProductResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                true
        );
        when(productService.linkProductToBusiness(any(LinkBusinessProductRequest.class))).thenReturn(ResponseEntity.ok(response));

        ResponseEntity<LinkBusinessProductResponse> result = productApi.linkProductToBusiness(request);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, result.getStatusCode());
    }
}
