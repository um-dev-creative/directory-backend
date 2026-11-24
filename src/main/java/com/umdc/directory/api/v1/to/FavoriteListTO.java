package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/// A favorite list with the number of active favorites it holds.
public record FavoriteListTO(
        @JsonProperty("id") UUID id,
        @JsonProperty("name") String name,
        @JsonProperty("item_count") long itemCount,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt) {
}
