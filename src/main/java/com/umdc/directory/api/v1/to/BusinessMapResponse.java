package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/// GeoJSON `FeatureCollection` of the map search.
///
/// @param type      always `FeatureCollection`
/// @param features  points (zoom 14 and above) or clusters (below 14)
/// @param truncated `true` when the limit was reached and more results exist
/// @param clustered `true` when the features are clusters
/// @param zoom      the zoom the answer was built for
public record BusinessMapResponse(
        @JsonProperty("type") String type,
        @JsonProperty("features") List<MapFeatureTO> features,
        @JsonProperty("truncated") boolean truncated,
        @JsonProperty("clustered") boolean clustered,
        @JsonProperty("zoom") int zoom) {
}
