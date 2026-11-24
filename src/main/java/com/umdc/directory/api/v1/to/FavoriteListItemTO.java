package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/// A favorite inside a list. `type` is `STORE` or `PRODUCT`; `item_id` is the store or product id.
public record FavoriteListItemTO(
        @JsonProperty("favorite_id") UUID favoriteId,
        @JsonProperty("type") String type,
        @JsonProperty("item_id") UUID itemId,
        @JsonProperty("name") String name,
        @JsonProperty("added_at") LocalDateTime addedAt) {
}
