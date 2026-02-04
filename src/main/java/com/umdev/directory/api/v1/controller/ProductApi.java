package com.umdev.directory.api.v1.controller;

import com.umdev.directory.api.v1.service.ProductService;
import com.umdev.directory.constant.DirectoryAppConstants;
import com.umdev.directory.api.v1.to.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// REST controller for product-related operations.
@Tag(name = "products", description = "The Product API")
public interface ProductApi {

    // Provides an instance of ProductService.
    //
    // @return an instance of ProductService.
    default ProductService getProductService() {
        // The controller could override the default implementation
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
        // The controller could override the default implementation
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
        // The controller could override the default implementation
        return getProductService().linkProductToBusiness(businessProductLinkCreateRequest);
    }

    // Lists products for a business with pagination.
    //
    // @param businessId the UUID of the business
    // @param page the page number (1-based)
    // @param perPage the number of items per page
    // @param active filter by active status (true/false)
    // @return a ResponseEntity containing the paginated list of products
    @Operation(summary = "List products for a business", description = "Returns paginated products for the given business id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductListResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content),
            @ApiResponse(responseCode = "404", description = "Business not found", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @GetMapping(path = "/business/{businessId}", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<ProductListResponse> findByBusinessId(
            @PathVariable @Parameter(description = "Business UUID") UUID businessId,
            @Parameter(description = "Page number (1-based). Default 1") @RequestParam(name = "page", required = false) Integer page,
            @Parameter(description = "Items per page. Default 10, max 100") @RequestParam(name = "per_page", required = false) Integer perPage,
            @Parameter(description = "Filter by active status (true/false)") @RequestParam(name = "active", required = false) Boolean active
    ) {
        return getProductService().findByBusinessId(businessId, page, perPage, active);
    }

}
