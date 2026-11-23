package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/// The saved searches of the caller, newest first.
///
/// @param items saved searches
/// @param max   how many a user may keep
public record SavedSearchListResponse(
        @JsonProperty("items") List<SavedSearchTO> items,
        @JsonProperty("max") int max) {
}
