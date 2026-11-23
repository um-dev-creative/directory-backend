package com.umdc.directory.api.v1.controller;

import com.umdc.commons.util.HttpStatusUtil;
import com.umdc.directory.api.v1.service.BusinessEventService;
import com.umdc.directory.api.v1.to.BusinessEventRequestTO;
import com.umdc.directory.api.v1.to.BusinessEventSummaryTO;
import com.umdc.directory.constant.DirectoryAppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/// Aggregated click events of a business: counting (any session) and the summary (owner only).
@Tag(name = "Business events", description = "Aggregated clicks (route, call, web, save) without personal data")
public interface BusinessEventApi {

    /// Name of the `{businessId}` path variable of the controller mapping.
    String BUSINESS_ID = "businessId";

    default BusinessEventService getService() {
        return new BusinessEventService() {};
    }

    @Operation(summary = "Count a click event of a business",
            description = "Adds one to today's (UTC) counter of the business, the optional campaign and the type (ROUTE, CALL, WEB, SAVE). "
                    + "Nothing about the client is stored; an event by the owner is not counted. Rate limited per client.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.NO_CONTENT_STR, description = "Counted (or ignored for the owner)", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.BAD_REQUEST_STR, description = "Missing body or invalid type", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = "Business, or campaign of that business, not found", content = @Content),
            @ApiResponse(responseCode = "429", description = "Too many events from this client", content = @Content)
    })
    @PostMapping(path = "/events", consumes = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<Void> recordEvent(
            @PathVariable(BUSINESS_ID) UUID businessId,
            @RequestBody(required = false) BusinessEventRequestTO request,
            @Parameter(hidden = true) @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return getService().recordEvent(businessId, request, sessionToken);
    }

    @Operation(summary = "Aggregated events of a business",
            description = "Counters per type, per campaign and per day (zeros included) between two UTC days (default: last 30 days, max 365). Owner only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "The summary"),
            @ApiResponse(responseCode = HttpStatusUtil.BAD_REQUEST_STR, description = "Invalid range", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.FORBIDDEN_STR, description = "The caller is not the owner of the business", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = "Business not found", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @GetMapping(path = "/events/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<BusinessEventSummaryTO> summary(
            @PathVariable(BUSINESS_ID) UUID businessId,
            @Parameter(description = "First day, yyyy-MM-dd") @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Last day, yyyy-MM-dd. Default today (UTC)") @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(hidden = true) @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return getService().summary(businessId, from, to, sessionToken);
    }
}
