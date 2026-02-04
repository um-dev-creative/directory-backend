package com.umdev.directory.api.v1.controller;

import com.umdev.directory.api.v1.service.CategoryService;
import com.umdev.directory.api.v1.to.CategoryCreateRequest;
import com.umdev.directory.api.v1.to.CategoryCreateResponse;
import com.umdev.directory.api.v1.to.CategoryGetResponse;
import com.umdev.directory.api.v1.to.PaginatedResponse;
import com.umdev.directory.constant.DirectoryAppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.UUID;

//
// REST controller for category-related operations.
//
// @see CategoryService
// @see CategoryGetResponse
@Schema(description = "REST controller for category-related operations. @see CategoryService @see CategoryGetResponse")
@Tag(name = "categories", description = "The Category API")
public interface CategoryApi {

    // Provides an instance of CategoryService.
    //
    // @return an instance of CategoryService
    default CategoryService getService() {
        return new CategoryService() {
        };
    }

    // Finds a category by its ID.
    //
    // @param categoryId the ID of the category to find
    // @return a ResponseEntity containing the response of the category find operation
    @Operation(summary = "Find category by ID", description = "Finds a category by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryGetResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @GetMapping(path = "/{categoryId}", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<CategoryGetResponse> find(@NotNull @PathVariable UUID categoryId) {
        return getService().find(categoryId);
    }

    // Finds categories by their parent ID with pagination support.
    //
    // @param parentId the parent ID of the categories to find
    // @param page the page number (0-based, default 0)
    // @param size the page size (default 20, max 100)
    // @return a ResponseEntity containing the paginated response of the categories find operation
    @Operation(summary = "Find categories by parent ID", 
               description = "Finds all categories that have the specified parent ID. Supports pagination for handling large datasets.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories found successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaginatedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid parent ID format", content = @Content),
            @ApiResponse(responseCode = "404", description = "Parent category not found", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @GetMapping(path = "/parent/{parentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<PaginatedResponse<CategoryGetResponse>> findByParentId(
            @NotNull @PathVariable UUID parentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return getService().findByParentId(parentId, page, size);
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
            // Removed 404 response as it is not appropriate for findAll operation
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @GetMapping
    default ResponseEntity<Collection<CategoryGetResponse>> findAll() {
        return getService().findAll();
    }

    // Creates a new category.
    //
    // @param request the request object containing the data for the category to create
    // @return a ResponseEntity containing the response of the category create operation
    @Operation(summary = "Create category", description = "Creates a new category.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Category successfully created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "404", description = "Parent category not found", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<CategoryCreateResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return getService().create(request);
    }
}
