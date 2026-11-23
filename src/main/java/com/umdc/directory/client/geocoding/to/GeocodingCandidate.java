package com.umdc.directory.client.geocoding.to;

/// A provider-independent coordinate candidate.
public record GeocodingCandidate(String label, double latitude, double longitude, Double confidence) {
}
