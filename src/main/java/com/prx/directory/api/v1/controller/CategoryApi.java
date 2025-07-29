package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.CategoryService;
import com.prx.directory.api.v1.to.CategoryGetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Collection;
import java.util.UUID;

/// REST controller for category-related operations.
///
/// @see CategoryService
/// @see CategoryGetResponse
@Schema(description = "/ REST controller for category-related operations.// @see CategoryService/ @see CategoryGetResponse")
@Tag(name = "categories", description = "The Category API")
public interface CategoryApi {

    /// Provides an instance of CategoryService.
    ///
    /// @return an instance of CategoryService
    default CategoryService getService() {
        return new CategoryService() {
        };
    }

    /// Finds a category by its ID.
    ///
    /// @param categoryId the ID of the category to find
    /// @return a ResponseEntity containing the response of the category find operation
    @Operation(summary = "Find category by ID", description = "Finds a category by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryGetResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping(path = "/{categoryId}", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<CategoryGetResponse> find(@NotNull @PathVariable UUID categoryId) {
        return getService().find(categoryId);
    }

    /// Finds categories by their parent ID.
    ///
    /// @param parentId the parent ID of the categories to find
    /// @return a ResponseEntity containing the response of the categories find operation
    @Operation(summary = "Find categories by parent ID", description = "Finds categories by their parent ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Collection.class))),
            @ApiResponse(responseCode = "404", description = "Categories not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping(path = "/parent/{parentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<Collection<CategoryGetResponse>> findByParentId(@NotNull @PathVariable UUID parentId) {
        return getService().findByParentId(parentId);
    }

    /**
     * Retrieves all categories.
     *
     * @return a {@link ResponseEntity} containing a collection of {@link CategoryGetResponse} objects
     */
    @Operation(summary = "Retrieve all categories", description = "Returns a collection of all available categories.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Collection.class))),
            @ApiResponse(responseCode = "404", description = "No categories found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping
    default ResponseEntity<Collection<CategoryGetResponse>> findAll() {
        return getService().findAll();
    }
}
