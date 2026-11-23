package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/// GeoJSON `Point` geometry; `coordinates` is `[longitude, latitude]`.
///
/// @param type        always `Point`
/// @param coordinates `[lng, lat]`, 5 decimals
public record GeoPointTO(
        @JsonProperty("type") String type,
        @JsonProperty("coordinates") List<Double> coordinates) {

    /// @param lng longitude
    /// @param lat latitude
    /// @return a point rounded to 5 decimals (about one metre)
    public static GeoPointTO of(double lng, double lat) {
        return new GeoPointTO("Point", List.of(round5(lng), round5(lat)));
    }

    private static double round5(double value) {
        return Math.round(value * 100_000d) / 100_000d;
    }
}
