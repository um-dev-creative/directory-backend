package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.LinkBusinessProductRequest;
import com.prx.directory.api.v1.to.LinkBusinessProductResponse;
import com.prx.directory.api.v1.to.ProductCreateRequest;
import com.prx.directory.api.v1.to.ProductCreateResponse;
import com.prx.directory.api.v1.to.ProductListItemTO;
import com.prx.directory.api.v1.to.ProductListResponse;
import com.prx.directory.constant.DirectoryAppConstants;
import com.prx.directory.jpa.entity.BusinessEntity;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("ProductServiceImpl - unit tests for product business logic")
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

    @Test
    @DisplayName("findByBusinessId - null businessId returns BAD_REQUEST")
    void findByBusinessId_nullBusinessId() {
        ResponseEntity<ProductListResponse> resp = productService.findByBusinessId(null, 1, 10, null);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @DisplayName("findByBusinessId - invalid page/perPage values return BAD_REQUEST")
    void findByBusinessId_invalidPageOrSize() {
        UUID businessId = UUID.randomUUID();
        // page less than default
        ResponseEntity<ProductListResponse> resp1 = productService.findByBusinessId(businessId, 0, 10, null);
        assertEquals(HttpStatus.BAD_REQUEST, resp1.getStatusCode());

        // perPage too small
        ResponseEntity<ProductListResponse> resp2 = productService.findByBusinessId(businessId, 1, 0, null);
        assertEquals(HttpStatus.BAD_REQUEST, resp2.getStatusCode());

        // perPage too large
        ResponseEntity<ProductListResponse> resp3 = productService.findByBusinessId(businessId, 1, DirectoryAppConstants.MAX_PER_PAGE + 1, null);
        assertEquals(HttpStatus.BAD_REQUEST, resp3.getStatusCode());
    }

    @Test
    @DisplayName("findByBusinessId - business not found returns NOT_FOUND")
    void findByBusinessId_businessNotFound() {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.existsById(businessId)).thenReturn(false);

        ResponseEntity<ProductListResponse> resp = productService.findByBusinessId(businessId, 1, 10, null);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    @DisplayName("findByBusinessId - active null uses findByBusiness and returns mapped items")
    void findByBusinessId_activeNull() {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.existsById(businessId)).thenReturn(true);

        BusinessProductEntity bp = new BusinessProductEntity();
        ProductEntity p = new ProductEntity();
        p.setId(UUID.randomUUID());
        bp.setProduct(p);
        Page<BusinessProductEntity> page = new org.springframework.data.domain.PageImpl<>(List.of(bp));

        when(businessProductRepository.findByBusiness(any(BusinessEntity.class), any(Pageable.class))).thenReturn(page);
        when(productMapper.toProductListItemTO(any(ProductEntity.class))).thenReturn(new ProductListItemTO(p.getId(), "name", "desc", null, null, null, null));

        ResponseEntity<ProductListResponse> resp = productService.findByBusinessId(businessId, 1, 10, null);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().data().size());
    }

    @Test
    @DisplayName("findByBusinessId - active specified uses findByBusinessAndActive and filters out null products")
    void findByBusinessId_activeSpecified() {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.existsById(businessId)).thenReturn(true);

        BusinessProductEntity bp1 = new BusinessProductEntity();
        ProductEntity p1 = new ProductEntity();
        p1.setId(UUID.randomUUID());
        bp1.setProduct(p1);

        BusinessProductEntity bp2 = new BusinessProductEntity();
        // simulate null product entry that should be filtered
        bp2.setProduct(null);

        Page<BusinessProductEntity> page = new org.springframework.data.domain.PageImpl<>(List.of(bp1, bp2));

        when(businessProductRepository.findByBusinessAndActive(any(BusinessEntity.class), eq(true), any(Pageable.class))).thenReturn(page);
        when(productMapper.toProductListItemTO(any(ProductEntity.class))).thenReturn(new ProductListItemTO(p1.getId(), "name", "desc", null, null, null, null));

        ResponseEntity<ProductListResponse> resp = productService.findByBusinessId(businessId, 1, 10, true);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().data().size());
    }

}
