package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.FavoriteService;
import com.prx.directory.api.v1.to.FavoriteCreateRequest;
import com.prx.directory.api.v1.to.FavoriteResponse;
import com.prx.directory.api.v1.to.FavoritesResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import static com.prx.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * REST API contract for managing "favorites" for authenticated users.
 *
 * <p>
 * This interface defines the endpoint for creating a Favorite (bookmark) that
 * associates the authenticated user with an item (store, product, or offer).
 * Implementations should:
 * <ul>
 *     <li>Authenticate the caller (session token header defined by {@code SESSION_TOKEN_KEY}).</li>
 *     <li>Validate the request payload (type and itemId).</li>
 *     <li>Verify that the referenced item exists (return 404 if not).</li>
 *     <li>Ensure idempotency: creating the same favorite twice should not produce duplicates.
 *         The API returns 201 Created for a new favorite and 409 CONFLICT for subsequent identical requests.</li>
 *     <li>Persist the favorite and return the created resource (FavoriteResponse) on creation.</li>
 * </ul>
 * </p>
 *
 * <p>
 * OpenAPI/Swagger annotations are used on the operation to describe request/response
 * shapes and status codes. The API is tagged under "favorites".
 * </p>
 *
 * @see FavoriteService
 * @see FavoriteCreateRequest
 * @see FavoriteResponse
 */
@Tag(name = "favorites", description = "Manage user favorites")
public interface FavoriteApi {

    /**
     * Obtain the {@link FavoriteService} that performs business logic for favorites.
     *
     * <p>
     * Default implementation returns a minimal anonymous instance. Controllers implementing
     * this interface should override this method to return a concrete, injected service bean.
     * </p>
     *
     * @return the FavoriteService used by the controller
     */
    default FavoriteService getService() {
        return new FavoriteService() {
        };
    }

    /**
     * Add a favorite item for the authenticated user.
     *
     * <p>Request JSON example:
     * <pre>
     * {
     *   "type": "STORE" | "PRODUCT" | "OFFER",
     *   "itemId": "uuid-string"
     * }
     * </pre>
     * </p>
     *
     * <p>Behavior and HTTP semantics:
     * <ul>
     *   <li>Requires authentication. The session token header is expected under the header name
     *       defined by {@link com.prx.security.constant.ConstantApp#SESSION_TOKEN_KEY}.</li>
     *   <li>Validates that the referenced item exists: if not found, return 404 Not Found.</li>
     *   <li>Is idempotent: repeated requests with the same user+type+itemId should not create duplicates.
     *       First successful creation should return 201 Created with the created {@link FavoriteResponse}.
     *       Subsequent identical requests will return 409 Conflict to indicate the favorite already exists,
     *       as per project policy for handling duplicates and idempotency.</li>
     *   <li>On validation errors return 400 Bad Request; on unauthenticated requests return 401 Unauthorized.</li>
     * </ul>
     * </p>
     *
     * @param sessionToken the session token header value identifying the authenticated user;
     *                     header name is {@value com.prx.security.constant.ConstantApp#SESSION_TOKEN_KEY}
     * @param request      the create request containing favorite {@code type} and {@code itemId}
     * @return a ResponseEntity containing the created {@link FavoriteResponse} when a favorite
     * is newly created (201), or an appropriate success/status code for existing favorites
     * or errors (400, 401, 404, 409 depending on outcome and project policy).
     */
    @Operation(summary = "Add favorite", description = "Add a favorite item (store, product, offer) for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Favorite created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FavoriteResponse.class))),
            @ApiResponse(responseCode = "409", description = "Favorite already exists (duplicate)", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Referenced item not found", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<FavoriteResponse> createFavorite(
            @RequestHeader(SESSION_TOKEN_KEY) String sessionToken,
            @Valid @RequestBody FavoriteCreateRequest request) {
        return this.getService().createFavorite(sessionToken, request);
    }

    /**
     * Get the authenticated user's favorites grouped by type. Supports optional filtering by type
     * and pagination (page, size) and optional sort string (e.g. "name,asc").
     *
     * @param sessionToken session token header
     * @param type optional filter: "stores" | "products" | "offers"
     * @param page page number (default 0)
     * @param size page size (default 10)
     * @param sort optional sort string
     * @return grouped favorites response
     */
    @Operation(summary = "Get user favorites", description = "Returns the authenticated user's favorites grouped by type (stores, products, offers). Supports pagination and optional filtering by type.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Favorites retrieved",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FavoritesResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<FavoritesResponse> getFavorites(
            @RequestHeader(SESSION_TOKEN_KEY) String sessionToken,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return this.getService().getFavorites(sessionToken, type, page, size, sort);
    }
}
