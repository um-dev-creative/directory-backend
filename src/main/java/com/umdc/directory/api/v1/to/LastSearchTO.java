package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/// The last search of the caller.
public record LastSearchTO(
        @JsonProperty("params") SearchParamsTO params,
        @JsonProperty("updated_at") LocalDateTime updatedAt) {
}
