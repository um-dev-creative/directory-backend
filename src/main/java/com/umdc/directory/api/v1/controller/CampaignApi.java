package com.umdc.directory.api.v1.controller;

import com.umdc.directory.api.v1.service.CampaignService;
import com.umdc.directory.api.v1.to.CampaignListResponse;
import com.umdc.directory.api.v1.to.CampaignTO;
import com.umdc.directory.api.v1.to.CampaignUpdateRequest;
import com.umdc.directory.api.v1.to.CampaignUpdateResponse;
import com.umdc.directory.constant.DirectoryAppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

@Tag(name = "campaigns", description = "The Campaign API")
public interface CampaignApi {

    // Avoid duplicate string literal warnings in annotations
    String HTTP_400 = "400";
    String HTTP_200 = "200";
    String HTTP_401 = "401";
    String HTTP_403 = "403";
    String HTTP_404 = "404";
    String HTTP_409 = "409";
    String MSG_NOT_FOUND = "Campaign not found";
    String MSG_BAD_TOKEN = "Missing or invalid session token";
    String MSG_NOT_OWNER = "The user does not own the campaign's business";
    String PARAM_CAMPAIGN_UUID = "Campaign UUID";

    /** Answers shared by deactivate, reactivate and publish (owner-only lifecycle calls). */
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaign updated; the body carries the computed status",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CampaignTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid session token", content = @Content),
            @ApiResponse(responseCode = "403", description = "The user does not own the campaign's business", content = @Content),
            @ApiResponse(responseCode = "404", description = "Campaign not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Status transition not allowed", content = @Content)
    })
    @interface LifecycleResponses {
    }

    /**
     * Provides a default instance of the CampaignService.
     *
     * @return an instance of CampaignService for handling campaign-related operations
     */
    default CampaignService getService() {
        return new CampaignService() {
        };
    }

    @Operation(
            summary = "Create a new campaign",
            description = "Persists a new campaign. The server generates id, createdDate and lastUpdate.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Campaign create payload", required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CampaignTO.class),
                            examples = @ExampleObject(value = "{\"title\":\"Holiday Sale\",\"description\":\"Seasonal discount campaign\",\"startDate\":\"2025-11-25T00:00:00Z\",\"endDate\":\"2025-12-31T23:59:59Z\",\"categoryId\":\"11111111-2222-3333-4444-555555555555\",\"businessId\":\"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee\",\"active\":true}")
                    )))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Campaign created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CampaignTO.class))),
            @ApiResponse(responseCode = HTTP_400, description = "Validation error", content = @Content),
            @ApiResponse(responseCode = HTTP_404, description = "Related entity not found (category/business)", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<CampaignTO> create(@RequestBody CampaignTO campaignTO,
                                              @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return this.getService().create(campaignTO, sessionToken);
    }

    @Operation(summary = "Get campaign by id",
            description = "Returns a single campaign by its UUID. Returns 404 if not found or if it is hidden from the caller "
                    + "(draft, paused or expired campaigns are visible to the owner of the business only). The status is computed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HTTP_200, description = "Campaign found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CampaignTO.class))),
            @ApiResponse(responseCode = HTTP_400, description = "Invalid UUID", content = @Content),
            @ApiResponse(responseCode = HTTP_404, description = MSG_NOT_FOUND, content = @Content)
    })
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<CampaignTO> getById(
            @Parameter(description = PARAM_CAMPAIGN_UUID) @PathVariable("id") UUID id,
            @Parameter(description = "Optional session token: the owner also sees draft, paused and expired campaigns")
            @RequestHeader(name = SESSION_TOKEN_KEY, required = false) String sessionToken) {
        return getService().find(id, sessionToken);
    }

    @Operation(summary = "Search campaigns",
            description = """
                    Returns a paginated list of campaigns matching optional filters. \
                    Filters: title (partial, case-insensitive), category_fk (UUID), business_fk (UUID), active (boolean), \
                    start_from/start_to (ISO datetime), end_from/end_to (ISO datetime). \
                    Pagination: page (1-based, default 1), per_page (default 20, max 100). \
                    Sorting: sort by title,start_date,end_date,created_date; prefix with '-' for desc. Default -created_date. \
                    Also status (DRAFT, SCHEDULED, ACTIVE, PAUSED, EXPIRED) and type (PROMOTION, EVENT, ANNOUNCEMENT). \
                    Consumers (no session-token or not the owner) only see scheduled and active campaigns; the owner of a business also sees its draft, paused and expired ones.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = HTTP_200, description = "Successful response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CampaignListResponse.class))),
            @ApiResponse(responseCode = HTTP_400, description = "Invalid pagination, sort or filter parameters", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<CampaignListResponse> listCampaigns(
            @Parameter(description = "Page number (1-based). Default 1") @RequestParam(name = "page", required = false) Integer page,
            @Parameter(description = "Items per page. Default 20, max 100") @RequestParam(name = "per_page", required = false) Integer perPage,
            @Parameter(description = "Comma-separated sort fields. Prefix with '-' for descending. Allowed: title, start_date, end_date, created_date") @RequestParam(name = "sort", required = false) String sort,
            @Parameter(description = """
                    Additional search filters (preferred): title (partial, case-insensitive), category_fk (UUID), business_fk (UUID), active (boolean), \
                    start_from/start_to (ISO datetime), end_from/end_to (ISO datetime). \
                    Legacy/deprecated filter parameters (supported for backward compatibility, avoid using in new integrations): q (use 'title'), category_id (use 'category_fk'), business_id (use 'business_fk'), \
                    start_date_from/start_date_to (use 'start_from'/'start_to'), end_date_from/end_date_to (use 'end_from'/'end_to').""") @RequestParam Map<String, String> filters,
            @Parameter(description = "Optional session token: lets the owner see all the campaigns of its businesses")
            @RequestHeader(name = SESSION_TOKEN_KEY, required = false) String sessionToken
    ) {
        return getService().list(page, perPage, sort, filters, sessionToken);
    }

    @Operation(
            summary = "Update an existing campaign",
            description = "Updates an existing campaign. Supports partial updates (PATCH semantics). " +
                    "Only provided fields will be updated. Validates field lengths, date constraints, " +
                    "and foreign key references. Updates last_update timestamp automatically. " +
                    "Implements optimistic locking - include the current last_update value to prevent concurrent modifications.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Campaign update payload. All fields are optional.", required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CampaignUpdateRequest.class)))
                    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Campaign update accepted; returns id and lastUpdate",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CampaignUpdateResponse.class))),
            @ApiResponse(responseCode = HTTP_400, description = "Invalid payload data (validation error)", content = @Content),
            @ApiResponse(responseCode = HTTP_401, description = MSG_BAD_TOKEN, content = @Content),
            @ApiResponse(responseCode = HTTP_403, description = MSG_NOT_OWNER, content = @Content),
            @ApiResponse(responseCode = HTTP_404, description = "Campaign not found or referenced entity (category/business) not found", content = @Content),
            @ApiResponse(responseCode = HTTP_409, description = "Concurrency conflict - campaign was modified by another request", content = @Content),
            @ApiResponse(responseCode = DirectoryAppConstants.INTERNAL_SERVER_ERROR_CODE, description = DirectoryAppConstants.INTERNAL_SERVER_ERROR_MESSAGE, content = @Content)
    })
    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<CampaignUpdateResponse> update(
            @Parameter(description = PARAM_CAMPAIGN_UUID) @PathVariable("id") UUID id,
            @Valid @RequestBody CampaignUpdateRequest request,
            @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        // Delegate entirely to the service implementation. Service controls status and body.
        return getService().update(id, request, sessionToken);
    }

    @Operation(summary = "Deactivate (pause) a campaign",
            description = "Owner only. Sets active=false; the campaign becomes PAUSED and is hidden from consumers. "
                    + "Only ACTIVE and SCHEDULED campaigns can be paused.")
    @LifecycleResponses
    @PostMapping(path = "/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<CampaignTO> deactivate(@Parameter(description = PARAM_CAMPAIGN_UUID) @PathVariable("id") UUID id,
                                                  @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return getService().deactivate(id, sessionToken);
    }

    @Operation(summary = "Reactivate a paused campaign",
            description = "Owner only. Sets active=true; only a PAUSED campaign can be reactivated (an expired one cannot).")
    @LifecycleResponses
    @PostMapping(path = "/{id}/reactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<CampaignTO> reactivate(@Parameter(description = PARAM_CAMPAIGN_UUID) @PathVariable("id") UUID id,
                                                  @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return getService().reactivate(id, sessionToken);
    }

    @Operation(summary = "Publish a draft campaign",
            description = "Owner only. A DRAFT campaign becomes SCHEDULED or ACTIVE (from its dates); it fails when the end date has passed.")
    @LifecycleResponses
    @PostMapping(path = "/{id}/publish", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<CampaignTO> publish(@Parameter(description = PARAM_CAMPAIGN_UUID) @PathVariable("id") UUID id,
                                               @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        return getService().publish(id, sessionToken);
    }
}
