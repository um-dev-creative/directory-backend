package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.BusinessService;
import com.prx.directory.api.v1.to.BusinessCreateRequest;
import com.prx.directory.api.v1.to.BusinessCreateResponse;
import com.prx.directory.api.v1.to.BusinessTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/// REST controller for business-related operations.
@Tag(name = "businesses", description = "The Business API")
public interface BusinessApi {

    /// Provides an instance of BusinessService.
    ///
    /// @return an instance of BusinessService
    default BusinessService getService() {
        return new BusinessService() {};
    }

    /// Handles the creation of a new business.
    ///
    /// @param businessCreateRequest the request object containing business details
    /// @return a ResponseEntity containing the response of the business creation operation
    @Operation(summary = "Create a new business", description = "Handles the creation of a new business.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Business successfully created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = BusinessCreateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<BusinessCreateResponse> post(@RequestBody BusinessCreateRequest businessCreateRequest) {
        return this.getService().create(businessCreateRequest);
    }

    /// Finds a business by its ID.
    ///
    /// @param id the ID of the business to find
    /// @return a ResponseEntity containing the response of the business find operation
    @Operation(summary = "Find business by ID", description = "Finds a business by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Business found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = BusinessTO.class))),
        @ApiResponse(responseCode = "404", description = "Business not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/{id}")
    default ResponseEntity<BusinessTO> findBusiness(@PathVariable UUID id) {
        return this.getService().findById(id);
    }

    /// Finds businesses by user ID.
    ///
    /// @param userId the user ID of the businesses to find
    /// @param pageable the pagination information
    /// @return a ResponseEntity containing the response of the businesses find operation
    @Operation(summary = "Find businesses by user ID", description = "Finds businesses by their user ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Businesses found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "404", description = "Businesses not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping(path = "/user/{userId}")
    default ResponseEntity<Page<BusinessTO>> findByUserId(@PathVariable UUID userId, Pageable pageable) {
        return this.getService().findByUserId(userId, pageable);
    }

}
