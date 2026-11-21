package com.umdc.directory.api.v1.controller;

import com.umdc.commons.util.HttpStatusUtil;
import com.umdc.directory.api.v1.service.LocationService;
import com.umdc.directory.api.v1.to.LocationCreateRequest;
import com.umdc.directory.api.v1.to.LocationTO;
import com.umdc.directory.api.v1.to.LocationUpdateRequest;
import com.umdc.directory.constant.DirectoryAppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.List;
import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/// Locations (sedes) of a business. Listing is open to any session; creating, changing and deleting are for the owner of the business.
@Tag(name = "Locations", description = "Locations (sedes) of a business")
public interface LocationApi {

    /// Name of the `{businessId}` path variable of the controller mapping.
    String BUSINESS_ID = "businessId";

    default LocationService getService() {
        return new LocationService() {};
    }

    @Operation(summary = "List the locations of a business", description = "Returns the locations of the business, the main one first.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "The locations"),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = "Business not found", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<List<LocationTO>> list(@PathVariable(BUSINESS_ID) UUID businessId) {
        return getService().list(businessId);
    }

    @Operation(summary = "Create a location", description = "Adds a location to the business. The first one is always the main one; main=true on a later one replaces the previous main.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.CREATED_STR, description = "Location created"),
            @ApiResponse(responseCode = HttpStatusUtil.BAD_REQUEST_STR, description = "Invalid body or unknown city (reason in the message-error header)", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.FORBIDDEN_STR, description = "The caller is not the owner of the business", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = "Business not found", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<LocationTO> create(
            @PathVariable(BUSINESS_ID) UUID businessId,
            @RequestBody LocationCreateRequest request,
            @Parameter(hidden = true) @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return getService().create(businessId, request, sessionToken);
    }

    @Operation(summary = "Update a location", description = "Changes only the fields that are sent. main=true makes it the main location; main=false on the main one is rejected.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "Location updated"),
            @ApiResponse(responseCode = HttpStatusUtil.BAD_REQUEST_STR, description = "Invalid body or unknown city (reason in the message-error header)", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.FORBIDDEN_STR, description = "The caller is not the owner of the business", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = "Business or location not found", content = @Content)
    })
    @PatchMapping(path = "/{locationId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<LocationTO> update(
            @PathVariable(BUSINESS_ID) UUID businessId,
            @PathVariable("locationId") UUID locationId,
            @RequestBody LocationUpdateRequest request,
            @Parameter(hidden = true) @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return getService().update(businessId, locationId, request, sessionToken);
    }

    @Operation(summary = "Delete a location", description = "Removes the location; if it was the main one, the oldest remaining location becomes the main one.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.NO_CONTENT_STR, description = "Location deleted", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.FORBIDDEN_STR, description = "The caller is not the owner of the business", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = "Business or location not found", content = @Content)
    })
    @DeleteMapping(path = "/{locationId}")
    default ResponseEntity<Void> delete(
            @PathVariable(BUSINESS_ID) UUID businessId,
            @PathVariable("locationId") UUID locationId,
            @Parameter(hidden = true) @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return getService().delete(businessId, locationId, sessionToken);
    }
}
