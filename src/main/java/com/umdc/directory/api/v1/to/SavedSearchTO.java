package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/// A saved search of the caller.
public record SavedSearchTO(
        @JsonProperty("id") UUID id,
        @JsonProperty("name") String name,
        @JsonProperty("params") SearchParamsTO params,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt,
        @JsonProperty("last_used_at") LocalDateTime lastUsedAt) {
}
