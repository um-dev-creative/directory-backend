package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.ProductService;
import com.prx.directory.api.v1.to.LinkBusinessProductRequest;
import com.prx.directory.api.v1.to.LinkBusinessProductResponse;
import com.prx.directory.api.v1.to.ProductCreateRequest;
import com.prx.directory.api.v1.to.ProductCreateResponse;
import com.prx.directory.mapper.ProductMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    ProductMapper productMapper;

    @InjectMocks
    private ProductController productController;

    @Test
    @DisplayName("createProduct should return CREATED status with valid request")
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

        ResponseEntity<ProductCreateResponse> result = productController.createProduct(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("createProduct should return BAD_REQUEST status with invalid request")
    void createProductShouldReturnBadRequestStatusWithInvalidRequest() {
        when(productService.create(any())).thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        ResponseEntity<ProductCreateResponse> result = productController.createProduct(null);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    @DisplayName("linkProductToBusiness should return OK status with valid request")
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

        ResponseEntity<LinkBusinessProductResponse> result = productController.linkProductToBusiness(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("linkProductToBusiness should return BAD_REQUEST status with invalid request")
    void linkProductToBusinessShouldReturnBadRequestStatusWithInvalidRequest() {
        LinkBusinessProductRequest request = new LinkBusinessProductRequest(
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        when(productService.linkProductToBusiness(any(LinkBusinessProductRequest.class))).thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        ResponseEntity<LinkBusinessProductResponse> result = productController.linkProductToBusiness(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }
}
