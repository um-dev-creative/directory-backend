package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.CampaignService;
import com.prx.directory.api.v1.to.CampaignTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

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
                            examples = @ExampleObject(value = "{\n  \"name\": \"Holiday Sale\",\n  \"description\": \"Seasonal discount campaign\",\n  \"startDate\": \"2025-11-25T00:00:00Z\",\n  \"endDate\": \"2025-12-31T23:59:59Z\",\n  \"categoryId\": \"11111111-2222-3333-4444-555555555555\",\n  \"businessId\": \"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee\",\n  \"active\": true\n}")
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
}
