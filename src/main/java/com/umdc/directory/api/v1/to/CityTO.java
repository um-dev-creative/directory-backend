package com.umdc.directory.api.v1.to;

import java.util.UUID;

/// A city of the catalogue.
///
/// @param id       city id
/// @param name     city name
/// @param province province or state
/// @param country  ISO 3166-1 alpha-2 country code
public record CityTO(UUID id, String name, String province, String country) {
}
