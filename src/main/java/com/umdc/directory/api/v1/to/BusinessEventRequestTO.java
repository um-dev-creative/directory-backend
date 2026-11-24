package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/// Body of `POST /businesses/{id}/events`.
///
/// @param type       ROUTE, CALL, WEB or SAVE (case-insensitive)
/// @param campaignId optional campaign of the business the click came from
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusinessEventRequestTO(
        @JsonProperty("type") String type,
        @JsonProperty("campaign_id") UUID campaignId) {
}
