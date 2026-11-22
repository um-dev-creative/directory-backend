package com.umdc.directory.api.v1.to;

import java.util.List;
import java.util.UUID;

/// A business in the search results, with its locations.
///
/// @param id              business id
/// @param name            business name
/// @param description     business description
/// @param categoryId      its category
/// @param categoryName    name of its category
/// @param verified        whether the business is verified
/// @param profileImageRef public URL of the profile image, if any
/// @param locations       its locations, the main one first
public record BusinessSearchItemTO(
        UUID id,
        String name,
        String description,
        UUID categoryId,
        String categoryName,
        boolean verified,
        String profileImageRef,
        List<LocationTO> locations) {
}
