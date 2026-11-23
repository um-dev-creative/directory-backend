package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/// Whether a location is open at an instant and when that changes.
///
/// @param openNow  true when open at the instant
/// @param closesAt when open: next closing as an ISO-8601 offset date-time in the business zone (else omitted)
/// @param opensAt  when closed: next opening as an ISO-8601 offset date-time in the business zone (omitted when no hours or no change foreseeable)
/// @param timezone IANA zone used for the evaluation
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpeningStatusTO(
        @JsonProperty("open_now") boolean openNow,
        @JsonProperty("closes_at") String closesAt,
        @JsonProperty("opens_at") String opensAt,
        @JsonProperty("timezone") String timezone) {
}
