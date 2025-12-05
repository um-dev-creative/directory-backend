package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.ProductService;
import com.prx.directory.api.v1.to.LinkBusinessProductRequest;
import com.prx.directory.api.v1.to.LinkBusinessProductResponse;
import com.prx.directory.api.v1.to.ProductCreateRequest;
import com.prx.directory.api.v1.to.ProductCreateResponse;
import com.prx.directory.constant.DirectoryAppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// REST controller for product-related operations.
@Tag(name = "products", description = "The Product API")
public interface ProductApi {

    // Provides an instance of ProductService.
    //
    // @return an instance of ProductService.
    default ProductService getProductService() {
        // Default implementation could be overridden by the controller
        return new ProductService(){};
    }

    // Handles the creation of a new product.
    //
    // @param productCreateRequest the request object containing product details
    // @return a ResponseEntity containing the response of the product creation operation
    @Operation(summary = "Create a new product", description = "Handles the creation of a new product.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Product successfully createdAt",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ProductCreateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
        @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<ProductCreateResponse> createProduct(@RequestBody ProductCreateRequest productCreateRequest) {
        // Default implementation could be overridden by the controller
        return getProductService().create(productCreateRequest);
    }

    // Links a product to a business.
    //
    // @param businessProductLinkCreateRequest the request object containing link details
    // @return a ResponseEntity containing the response of the link operation
    @Operation(summary = "Link product to business", description = "Links a product to a business.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product successfully linked to business",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = LinkBusinessProductResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
        @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, path = "/link")
    default ResponseEntity<LinkBusinessProductResponse> linkProductToBusiness(@RequestBody LinkBusinessProductRequest businessProductLinkCreateRequest) {
        // Default implementation could be overridden by the controller
        return getProductService().linkProductToBusiness(businessProductLinkCreateRequest);
    }
}
