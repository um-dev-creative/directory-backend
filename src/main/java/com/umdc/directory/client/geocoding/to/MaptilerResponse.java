package com.umdc.directory.client.geocoding.to;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/// GeoJSON FeatureCollection of the Maptiler Geocoding API (only the fields used). `center` is `[longitude, latitude]`.
@JsonIgnoreProperties(ignoreUnknown = true)
public record MaptilerResponse(List<Feature> features) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(@JsonProperty("place_name") String placeName, List<Double> center, Double relevance) {
    }
}
