package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.FavoriteCreateRequest;
import com.prx.directory.api.v1.to.FavoriteResponse;
import com.prx.directory.api.v1.to.FavoritesResponse;
import org.springframework.http.ResponseEntity;

/**
 * Service contract for creating and managing user favorites.
 *
 * <p>The service is invoked by the web controller layer and is responsible for:
 * - validating the referenced item exists (store, product, or offer),
 * - associating the favorite with the authenticated user (from the provided session token),
 * - preventing duplicate favorites for the same user+item+type (idempotency), and
 * - persisting and returning the created Favorite resource.
 *
 * <p>Idempotency policy: creating an already-existing favorite will result in a 409 Conflict
 * response from the service/controller layer (the application uses conflict semantics for
 * duplicate favorites).
 */
public interface FavoriteService {

    /**
     * Create a favorite for the authenticated user.
     *
     * @param sessionToken the user's session token (passed from the controller's request header)
     * @param request      the request payload containing the favorite {@code type} and {@code itemId}
     * @return a ResponseEntity carrying the created {@link FavoriteResponse} and the appropriate HTTP
     * status code:
     * <ul>
     *     <li>201 Created - favorite was successfully created</li>
     *     <li>409 Conflict - favorite already exists for the user+item+type (idempotent duplicate)</li>
     *     <li>400 Bad Request - validation errors in the request payload</li>
     *     <li>401 Unauthorized - the session token is missing/invalid</li>
     *     <li>404 Not Found - the referenced item (store/product/offer) does not exist</li>
     * </ul>
     */
    default ResponseEntity<FavoriteResponse> createFavorite(String sessionToken, FavoriteCreateRequest request) {
        return ResponseEntity.status(501).build();
    }

    /**
     * Retrieve the authenticated user's favorites grouped by type.
     *
     * @param sessionToken session token header
     * @param type         optional filter: "stores" | "products" | "offers"
     * @param page         page number (0-based)
     * @param size         page size
     * @param sort         optional sort string
     * @return ResponseEntity with grouped favorites
     */
    default ResponseEntity<FavoritesResponse> getFavorites(String sessionToken, String type, int page, int size, String sort) {
        return ResponseEntity.status(501).build();
    }
}
