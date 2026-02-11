package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.LinkBusinessProductRequest;
import com.prx.directory.api.v1.to.LinkBusinessProductResponse;
import com.prx.directory.api.v1.to.ProductCreateRequest;
import com.prx.directory.api.v1.to.ProductCreateResponse;
import com.prx.directory.jpa.entity.BusinessProductEntity;
import com.prx.directory.jpa.entity.BusinessProductEntityId;
import com.prx.directory.jpa.entity.ProductEntity;
import com.prx.directory.jpa.repository.BusinessProductRepository;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.jpa.repository.ProductRepository;
import com.prx.directory.mapper.BusinessProductMapper;
import com.prx.directory.mapper.ProductMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(value = {SpringExtension.class})
class ProductServiceImplTest {

    @Mock
    ProductRepository productRepository;
    @Mock
    BusinessProductRepository businessProductRepository;
    @Mock
    ProductMapper productMapper;
    @Mock
    BusinessProductMapper businessProductMapper;
    @Mock
    BusinessRepository businessRepository;
    @InjectMocks
    ProductServiceImpl productService;

    @Test
    @DisplayName("Create product successfully")
    void createProductSuccessfully() {
        ProductCreateRequest request = new ProductCreateRequest("Example Product", "This is an example product description.", UUID.randomUUID());
        ProductEntity product = new ProductEntity();
        ProductCreateResponse response = new ProductCreateResponse(UUID.randomUUID(), "Example Product", "This is an example product description.", null, null, UUID.randomUUID());

        when(productMapper.toProductEntity(any(ProductCreateRequest.class))).thenReturn(product);
        when(productRepository.save(any(ProductEntity.class))).thenReturn(product);
        when(productMapper.toProductCreateResponse(any(ProductEntity.class))).thenReturn(response);

        ResponseEntity<ProductCreateResponse> result = productService.create(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("Create product with null request")
    void createProductWithNullRequest() {
        ResponseEntity<ProductCreateResponse> response = productService.create(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Link product to business successfully")
    void linkProductToBusinessSuccessfully() {
        LinkBusinessProductRequest request = new LinkBusinessProductRequest(UUID.randomUUID(), UUID.randomUUID());
        BusinessProductEntity entity = new BusinessProductEntity();
        entity.setId(new BusinessProductEntityId(UUID.randomUUID(), UUID.randomUUID()));
        LinkBusinessProductResponse response = new LinkBusinessProductResponse(UUID.randomUUID(), UUID.randomUUID(), null, null, true);

        when(businessProductMapper.toSource(any(LinkBusinessProductRequest.class))).thenReturn(entity);
        when(businessProductRepository.save(any(BusinessProductEntity.class))).thenReturn(entity);
        when(businessProductMapper.toLinkBusinessProductResponse(any(BusinessProductEntity.class))).thenReturn(response);

        ResponseEntity<LinkBusinessProductResponse> result = productService.linkProductToBusiness(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("Link product to business with null request")
    void linkProductToBusinessWithNullRequest() {
        ResponseEntity<LinkBusinessProductResponse> response = productService.linkProductToBusiness(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Link product to business with null entity id")
    void linkProductToBusinessWithNullEntityId() {
        LinkBusinessProductRequest request = new LinkBusinessProductRequest(UUID.randomUUID(), UUID.randomUUID());
        BusinessProductEntity entity = new BusinessProductEntity();

        when(businessProductMapper.toSource(any(LinkBusinessProductRequest.class))).thenReturn(entity);
        when(businessProductRepository.save(any(BusinessProductEntity.class))).thenReturn(entity);

        ResponseEntity<LinkBusinessProductResponse> result = productService.linkProductToBusiness(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

}
