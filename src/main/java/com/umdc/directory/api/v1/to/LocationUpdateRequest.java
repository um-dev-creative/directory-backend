package com.umdc.directory.api.v1.to;

import java.math.BigDecimal;
import java.util.UUID;

/// Body of the partial update of a location: only the fields that are sent change. The same limits as the creation apply.
///
/// @param cityId      new city of the catalogue
/// @param name        new name
/// @param addressLine new street address
/// @param postalCode  new postal code
/// @param phone       new phone
/// @param latitude    new latitude; send it with the longitude
/// @param longitude   new longitude; send it with the latitude
/// @param main        `true` makes it the main location (the previous one stops being it); `false` is rejected, promote another one instead
public record LocationUpdateRequest(
        UUID cityId,
        String name,
        String addressLine,
        String postalCode,
        String phone,
        BigDecimal latitude,
        BigDecimal longitude,
        Boolean main) {
}
