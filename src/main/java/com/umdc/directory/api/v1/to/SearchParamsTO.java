package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/// Parameters of a search of businesses: the ones of `/businesses/search` plus the geo ones of `/nearby` and `/map`.
///
/// @param q          text found in the name or the description
/// @param categoryId category id
/// @param cityId     city id
/// @param city       city name (search only)
/// @param lat        latitude of the centre (with `lng`)
/// @param lng        longitude of the centre (with `lat`)
/// @param radiusM    radius in metres (needs `lat` and `lng`)
/// @param bbox       `minLng,minLat,maxLng,maxLat` (not with `lat`/`lng`)
/// @param sort       `distance` or `name` (needs `lat` and `lng`)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchParamsTO(
        @JsonProperty("q") String q,
        @JsonProperty("category_id") String categoryId,
        @JsonProperty("city_id") String cityId,
        @JsonProperty("city") String city,
        @JsonProperty("lat") Double lat,
        @JsonProperty("lng") Double lng,
        @JsonProperty("radius_m") Integer radiusM,
        @JsonProperty("bbox") String bbox,
        @JsonProperty("sort") String sort) {
}
