package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

/// A page of the business search, in the same envelope as the product listing.
///
/// @param totalCount total number of businesses that match
/// @param page       current page (1-based)
/// @param perPage    page size
/// @param totalPages number of pages
/// @param data       the businesses of this page
public record BusinessSearchResponse(
        @JsonProperty("total_count") long totalCount,
        @JsonProperty("page") int page,
        @JsonProperty("per_page") int perPage,
        @JsonProperty("total_pages") int totalPages,
        @JsonProperty("data") Collection<BusinessSearchItemTO> data) {
}
