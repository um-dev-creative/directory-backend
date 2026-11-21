package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.CityService;
import com.umdc.directory.api.v1.to.CityTO;
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

import java.util.List;

/// Cities catalogue: the cities a business can have a location in and the search can filter by.
@Tag(name = "Cities", description = "Cities catalogue")
public interface CityApi {

    default CityService getService() {
        return new CityService() {};
    }

    @Operation(summary = "List cities", description = "Lists the cities of the catalogue ordered by name; both filters are optional.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The cities"),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<List<CityTO>> list(
            @Parameter(description = "ISO 3166-1 alpha-2 country code, e.g. CA") @RequestParam(name = "country", required = false) String country,
            @Parameter(description = "Text found in the city name") @RequestParam(name = "q", required = false) String text) {
        return getService().list(country, text);
    }
}
