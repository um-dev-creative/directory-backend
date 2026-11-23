package com.umdc.directory.api.v1.controller;

import com.umdc.commons.util.HttpStatusUtil;
import com.umdc.directory.api.v1.service.GeocodeService;
import com.umdc.directory.api.v1.to.GeocodeRequest;
import com.umdc.directory.api.v1.to.GeocodeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/// Server-side geocoding: an address in, up to 5 coordinate candidates out. Nothing is stored.
@Tag(name = "Geocoding", description = "Address to coordinate candidates")
public interface GeocodeApi {

    default GeocodeService getService() {
        return new GeocodeService() {};
    }

    @Operation(summary = "Geocode an address", description = "Returns up to 5 candidates. Authenticated; the reason of a failure is in the message-error header.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "The candidates (possibly empty)"),
            @ApiResponse(responseCode = HttpStatusUtil.BAD_REQUEST_STR, description = "Invalid body", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "502", description = "The provider failed", content = @Content),
            @ApiResponse(responseCode = "503", description = "Geocoding is disabled", content = @Content),
            @ApiResponse(responseCode = "504", description = "The provider timed out", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<GeocodeResponse> geocode(
            @RequestBody(required = false) GeocodeRequest request,
            @Parameter(hidden = true) @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken) {
        return getService().geocode(request, sessionToken);
    }
}
