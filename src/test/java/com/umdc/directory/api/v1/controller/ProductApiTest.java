package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.ProductService;
import com.umdc.directory.api.v1.to.LinkBusinessProductRequest;
import com.umdc.directory.api.v1.to.ProductCreateRequest;
import com.umdc.directory.api.v1.to.ProductCreateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ProductApiTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductApi productApi = new ProductApi() {};

    @Test
    @DisplayName("createProduct should return NOT_IMPLEMENTED status with valid request")
    void createProductShouldReturnCreatedStatusWithValidRequest() {
        ProductCreateRequest request = new ProductCreateRequest(
                "product1",
                "description",
                UUID.randomUUID()
        );

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

        ResponseEntity<?> result = productApi.linkProductToBusiness(request);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, result.getStatusCode());
    }
}
