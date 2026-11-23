package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/// Coordinate candidates (at most 5). Never stored.
public record GeocodeResponse(List<Candidate> candidates) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Candidate(String label, double latitude, double longitude, Double confidence) {
    }
}
