package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.ProductService;
import com.umdc.directory.api.v1.to.LinkBusinessProductRequest;
import com.umdc.directory.api.v1.to.LinkBusinessProductResponse;
import com.umdc.directory.api.v1.to.ProductCreateRequest;
import com.umdc.directory.api.v1.to.ProductCreateResponse;
import com.umdc.directory.api.v1.to.ProductListResponse;
import com.umdc.directory.api.v1.to.ProductUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController implements ProductApi {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public ResponseEntity<ProductCreateResponse> createProduct(@Valid ProductCreateRequest productCreateRequest,
                                                         @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return productService.create(productCreateRequest, sessionToken);
    }

    @Override
    public ResponseEntity<LinkBusinessProductResponse> linkProductToBusiness(@Valid LinkBusinessProductRequest businessProductLinkCreateRequest,
                                                                              @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return productService.linkProductToBusiness(businessProductLinkCreateRequest, sessionToken);
    }

    @Override
    public ResponseEntity<ProductListResponse> findByBusinessId(UUID businessId, Integer page, Integer perPage, Boolean active,
                                                                Boolean includeInactive, String sessionToken) {
        return productService.findByBusinessId(businessId, page, perPage, active, includeInactive, sessionToken);
    }

    @Override
    public ResponseEntity<ProductCreateResponse> patchProduct(UUID productId, ProductUpdateRequest request, String sessionToken) {
        return productService.update(productId, request, sessionToken, false);
    }

    @Override
    public ResponseEntity<ProductCreateResponse> putProduct(UUID productId, ProductUpdateRequest request, String sessionToken) {
        return productService.update(productId, request, sessionToken, true);
    }

    @Override
    public ResponseEntity<Void> deleteProduct(UUID productId, String sessionToken) {
        return productService.deactivate(productId, sessionToken);
    }
}
