package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

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
/// @param openNow         whether some location is open right now (its hours, in the business time zone); null when no location has hours
/// @param distanceM       metres from the `lat`/`lng` sent to its nearest location; null when the request carried no point or no location has coordinates
public record BusinessSearchItemTO(
        UUID id,
        String name,
        String description,
        UUID categoryId,
        String categoryName,
        boolean verified,
        String profileImageRef,
        List<LocationTO> locations,
        @JsonProperty("open_now") Boolean openNow,
        @JsonProperty("distance_m") Long distanceM) {

    /// Item without the optional `open_now` and `distance_m`.
    public BusinessSearchItemTO(UUID id, String name, String description, UUID categoryId, String categoryName, boolean verified,
                                String profileImageRef, List<LocationTO> locations) {
        this(id, name, description, categoryId, categoryName, verified, profileImageRef, locations, null, null);
    }
}
