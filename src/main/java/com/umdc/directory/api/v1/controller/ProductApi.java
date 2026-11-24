package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.ProductService;
import com.umdc.directory.api.v1.to.*;
import com.umdc.directory.constant.DirectoryAppConstants;
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

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

// REST controller for product-related operations.
@Tag(name = "products", description = "The Product API")
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // the OpenAPI response codes and texts repeat on every operation
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
        @ApiResponse(responseCode = "401", description = "Missing or invalid session token", content = @Content),
        @ApiResponse(responseCode = "403", description = "The user does not own any business", content = @Content),
        @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<ProductCreateResponse> createProduct(@RequestBody ProductCreateRequest productCreateRequest,
                                                                @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        // The controller could override the default implementation
        return getProductService().create(productCreateRequest, sessionToken);
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
        @ApiResponse(responseCode = "401", description = "Missing or invalid session token", content = @Content),
        @ApiResponse(responseCode = "403", description = "The user does not own the business", content = @Content),
        @ApiResponse(responseCode = "404", description = "Business not found", content = @Content),
        @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, path = "/link")
    default ResponseEntity<LinkBusinessProductResponse> linkProductToBusiness(@RequestBody LinkBusinessProductRequest businessProductLinkCreateRequest,
                                                                              @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        // The controller could override the default implementation
        return getProductService().linkProductToBusiness(businessProductLinkCreateRequest, sessionToken);
    }

    // Lists products for a business with pagination.
    //
    // @param businessId the UUID of the business
    // @param page the page number (1-based)
    // @param perPage the number of items per page
    // @param active filter by product status, only used when includeInactive is true
    // @param includeInactive true lists inactive products too (owner only, needs session-token)
    // @return a ResponseEntity containing the paginated list of products
    @Operation(summary = "List products for a business",
            description = "Public: only active products. With include_inactive=true the owner of the business gets all of them.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductListResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content),
            @ApiResponse(responseCode = "401", description = "include_inactive=true without a valid session token", content = @Content),
            @ApiResponse(responseCode = "403", description = "include_inactive=true and the user does not own the business", content = @Content),
            @ApiResponse(responseCode = "404", description = "Business not found", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @GetMapping(path = "/business/{businessId}", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<ProductListResponse> findByBusinessId(
            @PathVariable @Parameter(description = "Business UUID") UUID businessId,
            @Parameter(description = "Page number (1-based). Default 1") @RequestParam(name = "page", required = false) Integer page,
            @Parameter(description = "Items per page. Default 10, max 100") @RequestParam(name = "per_page", required = false) Integer perPage,
            @Parameter(description = "Filter by product status (true/false); only with include_inactive=true") @RequestParam(name = "active", required = false) Boolean active,
            @Parameter(description = "Owner only: include inactive products") @RequestParam(name = "include_inactive", required = false) Boolean includeInactive,
            @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken
    ) {
        return getProductService().findByBusinessId(businessId, page, perPage, active, includeInactive, sessionToken);
    }

    @Operation(summary = "Partially update a product", description = "Changes only the sent fields (name, description, category, price, currency, image, active).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data (price, currency, image, blank name)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid session token", content = @Content),
            @ApiResponse(responseCode = "403", description = "The user does not own the product", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @PatchMapping(path = "/{productId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<ProductCreateResponse> patchProduct(@PathVariable UUID productId,
                                                               @RequestBody ProductUpdateRequest request,
                                                               @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return getProductService().update(productId, request, sessionToken, false);
    }

    @Operation(summary = "Replace a product", description = "Full replacement: name, description and categoryId are required; an absent price, currency or image is cleared.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product replaced",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid session token", content = @Content),
            @ApiResponse(responseCode = "403", description = "The user does not own the product", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @PutMapping(path = "/{productId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<ProductCreateResponse> putProduct(@PathVariable UUID productId,
                                                             @RequestBody ProductUpdateRequest request,
                                                             @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return getProductService().update(productId, request, sessionToken, true);
    }

    @Operation(summary = "Deactivate a product", description = "Soft delete: the product becomes inactive (active=false) and can be reactivated with PATCH.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deactivated (also when it already was)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid session token", content = @Content),
            @ApiResponse(responseCode = "403", description = "The user does not own the product", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @DeleteMapping(path = "/{productId}")
    default ResponseEntity<Void> deleteProduct(@PathVariable UUID productId,
                                               @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return getProductService().deactivate(productId, sessionToken);
    }

}
