package com.umdc.directory.api.v1.controller;

import com.umdc.commons.util.HttpStatusUtil;
import com.umdc.directory.api.v1.service.FavoriteListService;
import com.umdc.directory.api.v1.to.FavoriteListDetailTO;
import com.umdc.directory.api.v1.to.FavoriteListItemRequest;
import com.umdc.directory.api.v1.to.FavoriteListItemTO;
import com.umdc.directory.api.v1.to.FavoriteListNameRequest;
import com.umdc.directory.api.v1.to.FavoriteListTO;
import com.umdc.directory.api.v1.to.FavoriteListsResponse;
import com.umdc.directory.constant.FavoriteListConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/// Favorite lists of the consumer. A missing or invalid session token answers 401; a user only sees their own lists.
@Tag(name = "Favorite lists", description = "Named lists of favorite stores and products of the authenticated consumer")
public interface FavoriteListApi {

    String UNAUTHORIZED_DESC = "Unauthorized";
    String NOT_FOUND_DESC = "Not found (or not the caller's)";
    String REASON_DESC = "Invalid parameter (reason in the message-error header)";
    String LISTS = "/favorite-lists";
    String LIST_BY_ID = "/favorite-lists/{id}";

    default FavoriteListService getService() {
        return new FavoriteListService() {};
    }

    @Operation(summary = "Create a favorite list", description = "At most 20 per user; names are unique per user, ignoring case.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.CREATED_STR, description = "Created"),
            @ApiResponse(responseCode = HttpStatusUtil.BAD_REQUEST_STR, description = REASON_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = UNAUTHORIZED_DESC, content = @Content),
            @ApiResponse(responseCode = "409", description = "Duplicate name or limit reached", content = @Content)
    })
    @PostMapping(path = LISTS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<FavoriteListTO> createFavoriteList(
            @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken,
            @RequestBody(required = false) FavoriteListNameRequest request) {
        return getService().create(sessionToken, request);
    }

    @Operation(summary = "List my favorite lists", description = "Newest first, with the number of active favorites of each.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "The lists"),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = UNAUTHORIZED_DESC, content = @Content)
    })
    @GetMapping(path = LISTS, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<FavoriteListsResponse> listFavoriteLists(
            @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken) {
        return getService().list(sessionToken);
    }

    @Operation(summary = "Get a favorite list", description = "The list with one page (0-based) of its active favorites, newest first.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "The list and its page"),
            @ApiResponse(responseCode = HttpStatusUtil.BAD_REQUEST_STR, description = REASON_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = UNAUTHORIZED_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = NOT_FOUND_DESC, content = @Content)
    })
    @GetMapping(path = LIST_BY_ID, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<FavoriteListDetailTO> getFavoriteList(
            @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken,
            @PathVariable("id") UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + FavoriteListConstants.DEFAULT_PAGE_SIZE) int size) {
        return getService().get(sessionToken, id, page, size);
    }

    @Operation(summary = "Rename a favorite list", description = "Same rules as the creation: unique per user, ignoring case.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "Renamed"),
            @ApiResponse(responseCode = HttpStatusUtil.BAD_REQUEST_STR, description = REASON_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = UNAUTHORIZED_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = NOT_FOUND_DESC, content = @Content),
            @ApiResponse(responseCode = "409", description = "Another list already has that name", content = @Content)
    })
    @PatchMapping(path = LIST_BY_ID, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<FavoriteListTO> renameFavoriteList(
            @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken,
            @PathVariable("id") UUID id,
            @RequestBody(required = false) FavoriteListNameRequest request) {
        return getService().rename(sessionToken, id, request);
    }

    @Operation(summary = "Delete a favorite list", description = "Deletes the list and its items; the favorites are kept.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.NO_CONTENT_STR, description = "Deleted"),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = UNAUTHORIZED_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = NOT_FOUND_DESC, content = @Content)
    })
    @DeleteMapping(path = LIST_BY_ID)
    default ResponseEntity<Void> deleteFavoriteList(
            @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken,
            @PathVariable("id") UUID id) {
        return getService().delete(sessionToken, id);
    }

    @Operation(summary = "Add a favorite to a list", description = "Only the caller's active store or product favorites; a favorite may be in several lists.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.CREATED_STR, description = "Added"),
            @ApiResponse(responseCode = HttpStatusUtil.BAD_REQUEST_STR, description = REASON_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = UNAUTHORIZED_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = NOT_FOUND_DESC, content = @Content),
            @ApiResponse(responseCode = "409", description = "Favorite inactive or already in the list", content = @Content)
    })
    @PostMapping(path = LIST_BY_ID + "/items", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<FavoriteListItemTO> addFavoriteListItem(
            @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken,
            @PathVariable("id") UUID id,
            @RequestBody(required = false) FavoriteListItemRequest request) {
        return getService().addItem(sessionToken, id, request);
    }

    @Operation(summary = "Remove a favorite from a list")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.NO_CONTENT_STR, description = "Removed"),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = UNAUTHORIZED_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = NOT_FOUND_DESC, content = @Content)
    })
    @DeleteMapping(path = LIST_BY_ID + "/items/{favoriteId}")
    default ResponseEntity<Void> removeFavoriteListItem(
            @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken,
            @PathVariable("id") UUID id,
            @PathVariable("favoriteId") UUID favoriteId) {
        return getService().removeItem(sessionToken, id, favoriteId);
    }
}
