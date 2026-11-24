package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.BusinessSearchService;
import com.umdc.directory.api.v1.to.BusinessSearchResponse;
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

import java.util.UUID;

/// Search of businesses by city, category and text.
@Tag(name = "Business search", description = "Search of businesses by city, category and text")
public interface BusinessSearchApi {

    default BusinessSearchService getService() {
        return new BusinessSearchService() {};
    }

    @Operation(summary = "Search businesses", description = "Paginated search; the filters that are sent are combined with AND. Results are ordered by name (or by distance). Public: no session-token needed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "A page of businesses with their locations"),
            @ApiResponse(responseCode = "400", description = "Invalid page, per_page, lat, lng or sort", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("PMD.UseObjectForClearerAPI") // the parameters are the query string
    default ResponseEntity<BusinessSearchResponse> search(
            @Parameter(description = "City id from GET /api/v1/cities") @RequestParam(name = "city_id", required = false) UUID cityId,
            @Parameter(description = "City name (ignored when city_id is sent)") @RequestParam(name = "city", required = false) String city,
            @Parameter(description = "Category id; its sub-categories are included") @RequestParam(name = "category_id", required = false) UUID categoryId,
            @Parameter(description = "Text found in the name or the description") @RequestParam(name = "q", required = false) String text,
            @Parameter(description = "Page number (1-based). Default 1") @RequestParam(name = "page", required = false) Integer page,
            @Parameter(description = "Items per page. Default 10, max 100") @RequestParam(name = "per_page", required = false) Integer perPage,
            @Parameter(description = "true keeps only the businesses open right now") @RequestParam(name = "open_now", required = false) Boolean openNow,
            @Parameter(description = "Latitude of the caller (with lng): fills distance_m") @RequestParam(name = "lat", required = false) String lat,
            @Parameter(description = "Longitude of the caller (with lat)") @RequestParam(name = "lng", required = false) String lng,
            @Parameter(description = "name (default) or distance (needs lat and lng)") @RequestParam(name = "sort", required = false) String sort) {
        return getService().search(cityId, city, categoryId, text, page, perPage, openNow, lat, lng, sort);
    }
}
