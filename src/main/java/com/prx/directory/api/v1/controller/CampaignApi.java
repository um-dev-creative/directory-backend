package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.CampaignService;
import com.prx.directory.api.v1.to.CampaignListResponse;
import com.prx.directory.api.v1.to.CampaignTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.UUID;

@Tag(name = "campaigns", description = "The Campaign API")
public interface CampaignApi {

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
            requestBody = @RequestBody(description = "Campaign create payload", required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CampaignTO.class),
                            examples = @ExampleObject(value = "{\n  \"name\": \"Holiday Sale\",\n  \"description\": \"Seasonal discount campaign\",\n  \"startDate\": \"2025-11-25T00:00:00Z\",\n  \"endDate\": \"2025-12-31T23:59:59Z\",\n  \"categoryId\": \"11111111-2222-3333-4444-555555555555\",\n  \"businessId\": \"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee\",\n  \"active\": true\n}" )
                    )))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Campaign created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CampaignTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Related entity not found (category/business)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<CampaignTO> create(@RequestBody CampaignTO campaignTO) {
        return this.getService().create(campaignTO);
    }

    @Operation(summary = "Get campaign by id",
            description = "Returns a single campaign by its UUID. Returns 404 if not found.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaign found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CampaignTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid UUID", content = @Content),
            @ApiResponse(responseCode = "404", description = "Campaign not found", content = @Content)
    })
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<CampaignTO> getById(
            @Parameter(description = "Campaign UUID") @PathVariable("id") UUID id) {
        return getService().find(id);
    }

    @Operation(summary = "Search campaigns",
            description = "Returns a paginated list of campaigns matching optional filters. " +
                    "Filters: name (partial, case-insensitive), category_fk (UUID), business_fk (UUID), active (boolean), " +
                    "start_from/start_to (ISO datetime), end_from/end_to (ISO datetime). " +
                    "Pagination: page (1-based, default 1), per_page (default 20, max 100). " +
                    "Sorting: sort by name,start_date,end_date,created_date; prefix with '-' for desc. Default -created_date.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CampaignListResponse.class),
                            examples = @ExampleObject(
                                    value = "{\n  \"total_count\": 1,\n  \"page\": 1,\n  \"per_page\": 20,\n  \"total_pages\": 1,\n  \"items\": [{\n    \"id\": \"...\",\n    \"name\": \"Holiday Sale\",\n    \"description\": \"...\",\n    \"businessId\": \"...\",\n    \"startDate\": \"2025-11-25T00:00:00Z\",\n    \"endDate\": \"2025-12-31T23:59:59Z\",\n    \"active\": true\n  }]\n}"))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination, sort or filter parameters", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<CampaignListResponse> listCampaigns(
            @Parameter(description = "Page number (1-based). Default 1") @RequestParam(name = "page", required = false) Integer page,
            @Parameter(description = "Items per page. Default 20, max 100") @RequestParam(name = "per_page", required = false) Integer perPage,
            @Parameter(description = "Comma-separated sort fields. Prefix with '-' for descending. Allowed: name, start_date, end_date, created_date") @RequestParam(name = "sort", required = false) String sort,
            @Parameter(description = "Additional search filters: name, category_fk, business_fk, active, start_from, start_to, end_from, end_to") @RequestParam Map<String, String> filters
    ) {
        return getService().list(page, perPage, sort, filters);
    }
}
