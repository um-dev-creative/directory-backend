package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/// What the client must call to rerun a saved search.
///
/// @param endpoint path to call with GET: `/api/v1/businesses/search`, `/nearby` or `/map`
/// @param params   query parameters, with the names that endpoint expects
public record SearchReplayTO(
        @JsonProperty("endpoint") String endpoint,
        @JsonProperty("params") Map<String, String> params) {
}
