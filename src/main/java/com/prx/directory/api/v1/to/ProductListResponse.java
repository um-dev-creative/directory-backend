package com.prx.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

/**
 * Represents the response structure for a paginated list of products.
 *
 * The ProductListResponse encapsulates essential data required for
 * managing paginated responses when fetching a list of products.
 * This record includes metadata about the pagination, such as the
 * total number of items, the current page, the number of items per page,
 * the total pages available, and the actual data representing the products.
 *
 * Fields:
 * - totalCount: The total number of products available across all pages.
 * - page: The current page number of the paginated response.
 * - perPage: The number of products provided per page.
 * - totalPages: The total number of pages available based on pagination settings.
 * - data: A collection of ProductListItemTO objects representing the products returned in the current page.
 */
public record ProductListResponse(
        @JsonProperty("total_count") long totalCount,
        @JsonProperty("page") int page,
        @JsonProperty("per_page") int perPage,
        @JsonProperty("total_pages") int totalPages,
        @JsonProperty("data") Collection<ProductListItemTO> data
) {}

