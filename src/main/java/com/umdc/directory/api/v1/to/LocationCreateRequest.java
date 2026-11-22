package com.umdc.directory.api.v1.to;

import java.math.BigDecimal;
import java.util.UUID;

/// Body of the creation of a location.
///
/// @param cityId      city of the catalogue (required)
/// @param name        optional name of the branch, up to 120 characters
/// @param addressLine street address (required, up to 200 characters)
/// @param postalCode  optional, up to 10 characters
/// @param phone       optional, up to 30 characters
/// @param latitude    optional, -90..90; must come with the longitude
/// @param longitude   optional, -180..180; must come with the latitude
/// @param main        whether it is the main location; the first location of a business is always the main one
public record LocationCreateRequest(
        UUID cityId,
        String name,
        String addressLine,
        String postalCode,
        String phone,
        BigDecimal latitude,
        BigDecimal longitude,
        Boolean main) {
}
