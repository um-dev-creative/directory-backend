package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/// The lists of the caller, newest first, and how many a user may keep.
public record FavoriteListsResponse(
        @JsonProperty("items") List<FavoriteListTO> items,
        @JsonProperty("max") int max) {
}
