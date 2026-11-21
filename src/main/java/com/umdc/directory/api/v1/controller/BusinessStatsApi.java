package com.umdc.directory.api.v1.controller;

import com.umdc.commons.util.HttpStatusUtil;
import com.umdc.directory.api.v1.service.BusinessStatsService;
import com.umdc.directory.api.v1.to.BusinessStatsTO;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/// Statistics of a business: view counting (any session) and the summary (owner only).
@Tag(name = "Business statistics", description = "Views of a business, without personal data")
public interface BusinessStatsApi {

    /// Name of the `{businessId}` path variable of the controller mapping.
    String BUSINESS_ID = "businessId";

    default BusinessStatsService getService() {
        return new BusinessStatsService() {};
    }

    @Operation(summary = "Count a view of a business",
            description = "Adds one view to today's (UTC) counter. Nothing about the viewer is stored; a view by the owner is not counted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.NO_CONTENT_STR, description = "Counted (or ignored for the owner)", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = "Business not found", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @PostMapping(path = "/views")
    default ResponseEntity<Void> recordView(
            @PathVariable(BUSINESS_ID) UUID businessId,
            @Parameter(hidden = true) @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return getService().recordView(businessId, sessionToken);
    }

    @Operation(summary = "Views summary of a business",
            description = "Total, today, last 7 days and one entry per day (zeros included) of a window of 1 to 365 days ending today (UTC). Owner only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HttpStatusUtil.OK_STR, description = "The summary"),
            @ApiResponse(responseCode = HttpStatusUtil.BAD_REQUEST_STR, description = "days out of range", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.UNAUTHORIZED_STR, description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.FORBIDDEN_STR, description = "The caller is not the owner of the business", content = @Content),
            @ApiResponse(responseCode = HttpStatusUtil.NOT_FOUND_STR, description = "Business not found", content = @Content)
    })
    @GetMapping(path = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<BusinessStatsTO> summary(
            @PathVariable(BUSINESS_ID) UUID businessId,
            @Parameter(description = "Window size in days, 1 to 365. Default 30") @RequestParam(name = "days", required = false) Integer days,
            @Parameter(hidden = true) @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return getService().summary(businessId, days, sessionToken);
    }
}
