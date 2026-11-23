package com.umdc.directory.api.v1.to;

/// Body of `POST /api/v1/geocode`. `address` is required (max 200); `city` and `country` (ISO alpha-2, default `ca`) are optional.
public record GeocodeRequest(String address, String city, String country) {
}
