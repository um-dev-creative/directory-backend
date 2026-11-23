package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/// GeoJSON `Feature`: a location (properties `business_id`, `location_id`, `name`, `category_id`, `verified`, `main`) or,
/// below zoom 14, a cluster (properties `cluster=true`, `count`).
///
/// @param type       always `Feature`
/// @param geometry   the point
/// @param properties the properties of the feature
public record MapFeatureTO(
        @JsonProperty("type") String type,
        @JsonProperty("geometry") GeoPointTO geometry,
        @JsonProperty("properties") Map<String, Object> properties) {
}
