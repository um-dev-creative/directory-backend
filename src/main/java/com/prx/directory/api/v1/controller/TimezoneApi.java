package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.TimezoneService;
import com.prx.directory.api.v1.to.GetTimezoneCollectionResponse;
import com.prx.directory.api.v1.to.TimezoneTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * TimezoneApi defines the contract for timezone-related endpoints.
 */
@RequestMapping("/api/v1/timezones")
public interface TimezoneApi {

    default TimezoneService getService() {
        return new TimezoneService() {
        };
    }

    /**
     * Retrieves a list of all available timezones.
     *
     * @return a ResponseEntity containing a list of TimezoneTO objects.
     */
    @GetMapping(path = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<GetTimezoneCollectionResponse> getAllTimezones() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Retrieves a paginated list of all supported timezones.
     *
     * @param pageable Pageable object for pagination and sorting
     * @return Page of TimezoneTO objects.
     */
    @Operation(summary = "Get all supported timezones (paginated)", description = "Returns a paginated list of all supported timezones.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of timezones returned successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    default ResponseEntity<Page<TimezoneTO>> getTimezones(Pageable pageable) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
