package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.BusinessGeoService;
import com.umdc.directory.api.v1.to.BusinessMapResponse;
import com.umdc.directory.api.v1.to.BusinessNearbyResponse;
import com.umdc.directory.constant.DirectoryAppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/// Public geographic search of businesses.
@Tag(name = "Business geo search", description = "Businesses near a point and businesses of a map area")
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public interface BusinessGeoApi {

    default BusinessGeoService getService() {
        return new BusinessGeoService() {};
    }

    @Operation(summary = "Locations near a point", description = "Locations within radius_m metres, one row per location with distance_m. Sorted by distance (ties by id) or by name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "A page of locations with their distance"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter; the reason is in the message-error header", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @GetMapping(path = "/nearby", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<BusinessNearbyResponse> nearby(
            @Parameter(description = "Latitude, -90 to 90 (required)") @RequestParam(name = "lat", required = false) String lat,
            @Parameter(description = "Longitude, -180 to 180 (required)") @RequestParam(name = "lng", required = false) String lng,
            @Parameter(description = "Radius in metres. Default 5000, max 50000") @RequestParam(name = "radius_m", required = false) String radiusM,
            @Parameter(description = "Category id; its sub-categories are included") @RequestParam(name = "category_id", required = false) String categoryId,
            @Parameter(description = "City id") @RequestParam(name = "city_id", required = false) String cityId,
            @Parameter(description = "Text found in the name or the description") @RequestParam(name = "q", required = false) String text,
            @Parameter(description = "distance (default) or name") @RequestParam(name = "sort", required = false) String sort,
            @Parameter(description = "Page number (1-based). Default 1") @RequestParam(name = "page", required = false) String page,
            @Parameter(description = "Items per page. Default 10, max 100") @RequestParam(name = "per_page", required = false) String perPage) {
        return getService().nearby(lat, lng, radiusM, categoryId, cityId, text, sort, page, perPage);
    }

    @Operation(summary = "Businesses of a map area", description = "GeoJSON FeatureCollection. Zoom 14 and above: one Point per location. Below 14: grid clusters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "GeoJSON FeatureCollection"),
            @ApiResponse(responseCode = "400", description = "Invalid, inverted or too large bbox, or another invalid parameter; reason in the message-error header", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @GetMapping(path = "/map", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<BusinessMapResponse> map(
            @Parameter(description = "minLng,minLat,maxLng,maxLat (required, at most 90 degrees per axis)") @RequestParam(name = "bbox", required = false) String bbox,
            @Parameter(description = "Zoom 0-22. Default 14") @RequestParam(name = "zoom", required = false) String zoom,
            @Parameter(description = "Category id; its sub-categories are included") @RequestParam(name = "category_id", required = false) String categoryId,
            @Parameter(description = "City id") @RequestParam(name = "city_id", required = false) String cityId,
            @Parameter(description = "Text found in the name or the description") @RequestParam(name = "q", required = false) String text,
            @Parameter(description = "Max features. Default 200, max 500") @RequestParam(name = "limit", required = false) String limit) {
        return getService().map(bbox, zoom, categoryId, cityId, text, limit);
    }
}
