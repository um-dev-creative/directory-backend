package com.prx.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

/**
 * Generic paginated response envelope used by list endpoints.
 * @param <T> item type
 */
public record PaginatedResponse<T>(
        @JsonProperty("total_count") long totalCount,
        @JsonProperty("page") int page,
        @JsonProperty("per_page") int perPage,
        @JsonProperty("total_pages") int totalPages,
        @JsonProperty("items") Collection<T> items
) {}

