package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/// A location (sede) found by the nearby search, with the business it belongs to and its distance to the searched point.
///
/// @param locationId      location id
/// @param businessId      business id
/// @param name            business name
/// @param description     business description
/// @param categoryId      category of the business
/// @param categoryName    name of that category
/// @param verified        whether the business is verified
/// @param profileImageRef public URL of the profile image, if any
/// @param cityId          city of the location
/// @param cityName        name of that city
/// @param locationName    optional name of the location
/// @param addressLine     street address
/// @param latitude        latitude in degrees
/// @param longitude       longitude in degrees
/// @param main            whether it is the main location of the business
/// @param distanceM       distance in metres from the searched point
public record BusinessNearbyItemTO(
        @JsonProperty("location_id") UUID locationId,
        @JsonProperty("business_id") UUID businessId,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("category_id") UUID categoryId,
        @JsonProperty("category_name") String categoryName,
        @JsonProperty("verified") boolean verified,
        @JsonProperty("profile_image_ref") String profileImageRef,
        @JsonProperty("city_id") UUID cityId,
        @JsonProperty("city_name") String cityName,
        @JsonProperty("location_name") String locationName,
        @JsonProperty("address_line") String addressLine,
        @JsonProperty("latitude") double latitude,
        @JsonProperty("longitude") double longitude,
        @JsonProperty("main") boolean main,
        @JsonProperty("distance_m") long distanceM) {
}
