package com.umdc.directory.api.v1.controller;

import com.umdc.commons.util.HttpStatusUtil;
import com.umdc.directory.api.v1.service.OpeningHoursService;
import com.umdc.directory.api.v1.to.OpeningHoursTO;
import com.umdc.directory.api.v1.to.OpeningStatusTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/// Opening hours of a location and its "open now" status.
@Tag(name = "Opening hours", description = "Opening hours per location and open-now status")
public interface OpeningHoursApi {

    String BUSINESS_ID = "businessId";
    String LOCATION_ID = "locationId";

    default OpeningHoursService getService() {
        return new OpeningHoursService() {};
    }

    @Operation(summary = "Opening hours of a location", description = "Public. Slots in week order, in local time of the business zone.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "The hours (slots empty when none were set)"),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = "Business or location not found", content = @Content)
    })
    @GetMapping(path = "/hours", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<OpeningHoursTO> getHours(
            @PathVariable(BUSINESS_ID) UUID businessId,
            @PathVariable(LOCATION_ID) UUID locationId) {
        return getService().get(businessId, locationId);
    }

    @Operation(summary = "Replace the opening hours of a location",
            description = "Owner only. Replaces the whole schedule: up to 4 non-overlapping slots per day, HH:mm, a closed day is one slot with closed=true.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "The stored hours"),
            @ApiResponse(responseCode = HttpStatusUtil.BAD_REQUEST_STR, description = "Invalid slots (reason in the message-error header)", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.FORBIDDEN_STR, description = "The caller is not the owner of the business", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = "Business or location not found", content = @Content)
    })
    @PutMapping(path = "/hours", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<OpeningHoursTO> putHours(
            @PathVariable(BUSINESS_ID) UUID businessId,
            @PathVariable(LOCATION_ID) UUID locationId,
            @RequestBody(required = false) OpeningHoursTO body,
            @Parameter(hidden = true) @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return getService().replace(businessId, locationId, body, sessionToken);
    }

    @Operation(summary = "Open-now status of a location", description = "Public. open_now plus the next closing (when open) or next opening (when closed), in the business zone.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "The status"),
            @ApiResponse(responseCode = HttpStatusUtil.BAD_REQUEST_STR, description = "at is not an ISO-8601 instant", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = "Business or location not found", content = @Content)
    })
    @GetMapping(path = "/hours/status", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<OpeningStatusTO> getStatus(
            @PathVariable(BUSINESS_ID) UUID businessId,
            @PathVariable(LOCATION_ID) UUID locationId,
            @Parameter(description = "ISO-8601 instant to evaluate; now when omitted") @RequestParam(name = "at", required = false) String at) {
        return getService().status(businessId, locationId, at);
    }
}
