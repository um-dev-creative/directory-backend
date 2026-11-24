package com.umdc.directory.api.v1.controller;

import com.umdc.commons.util.HttpStatusUtil;
import com.umdc.directory.api.v1.service.SavedSearchService;
import com.umdc.directory.api.v1.to.LastSearchTO;
import com.umdc.directory.api.v1.to.SavedSearchCreateRequest;
import com.umdc.directory.api.v1.to.SavedSearchListResponse;
import com.umdc.directory.api.v1.to.SavedSearchRenameRequest;
import com.umdc.directory.api.v1.to.SavedSearchTO;
import com.umdc.directory.api.v1.to.SearchParamsTO;
import com.umdc.directory.api.v1.to.SearchReplayTO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/// Saved searches and last search of the consumer. A missing or invalid session token answers 401; a user only sees their own rows.
@Tag(name = "Saved searches", description = "Saved searches and last search of the authenticated consumer")
public interface SavedSearchApi {

    String UNAUTHORIZED_DESC = "Unauthorized";
    String NOT_FOUND_DESC = "Saved search not found (or not the caller's)";
    String REASON_DESC = "Invalid parameter (reason in the message-error header)";

    default SavedSearchService getService() {
        return new SavedSearchService() {};
    }

    @Operation(summary = "Save a search", description = "Saves the name and the parameters (validated like /nearby and /map). At most 20 per user; names are unique per user, ignoring case.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.CREATED_STR, description = "Saved"),
            @ApiResponse(responseCode = HttpStatusUtil.BAD_REQUEST_STR, description = REASON_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = UNAUTHORIZED_DESC, content = @Content),
            @ApiResponse(responseCode = "409", description = "Duplicate name or limit reached", content = @Content)
    })
    @PostMapping(path = "/saved-searches", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<SavedSearchTO> createSavedSearch(
            @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken,
            @RequestBody(required = false) SavedSearchCreateRequest request) {
        return getService().create(sessionToken, request);
    }

    @Operation(summary = "List my saved searches", description = "Newest first.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "The saved searches"),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = UNAUTHORIZED_DESC, content = @Content)
    })
    @GetMapping(path = "/saved-searches", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<SavedSearchListResponse> listSavedSearches(
            @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken) {
        return getService().list(sessionToken);
    }

    @Operation(summary = "Rename a saved search")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "Renamed"),
            @ApiResponse(responseCode = HttpStatusUtil.BAD_REQUEST_STR, description = REASON_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = UNAUTHORIZED_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = NOT_FOUND_DESC, content = @Content),
            @ApiResponse(responseCode = "409", description = "Duplicate name", content = @Content)
    })
    @PatchMapping(path = "/saved-searches/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<SavedSearchTO> renameSavedSearch(
            @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken,
            @PathVariable("id") UUID id,
            @RequestBody(required = false) SavedSearchRenameRequest request) {
        return getService().rename(sessionToken, id, request);
    }

    @Operation(summary = "Delete a saved search", description = "Physical delete.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.NO_CONTENT_STR, description = "Deleted"),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = UNAUTHORIZED_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = NOT_FOUND_DESC, content = @Content)
    })
    @DeleteMapping(path = "/saved-searches/{id}")
    default ResponseEntity<Void> deleteSavedSearch(
            @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken,
            @PathVariable("id") UUID id) {
        return getService().delete(sessionToken, id);
    }

    @Operation(summary = "Replay a saved search", description = "Returns the endpoint and the query parameters to call to rerun the search, and stamps last_used_at.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "Endpoint and parameters"),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = UNAUTHORIZED_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = NOT_FOUND_DESC, content = @Content)
    })
    @GetMapping(path = "/saved-searches/{id}/replay", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<SearchReplayTO> replaySavedSearch(
            @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken,
            @PathVariable("id") UUID id) {
        return getService().replay(sessionToken, id);
    }

    @Operation(summary = "Get my last search")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "The last search"),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = UNAUTHORIZED_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = "The user has no last search", content = @Content)
    })
    @GetMapping(path = "/last-search", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<LastSearchTO> getLastSearch(
            @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken) {
        return getService().getLast(sessionToken);
    }

    @Operation(summary = "Remember my last search", description = "Upsert: one last search per user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "Stored"),
            @ApiResponse(responseCode = HttpStatusUtil.BAD_REQUEST_STR, description = REASON_DESC, content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = UNAUTHORIZED_DESC, content = @Content)
    })
    @PutMapping(path = "/last-search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<LastSearchTO> putLastSearch(
            @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken,
            @RequestBody(required = false) SearchParamsTO params) {
        return getService().putLast(sessionToken, params);
    }
}
