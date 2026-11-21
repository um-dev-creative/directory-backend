package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.CityTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/// Operations on the cities catalogue.
public interface CityService {

    /// Lists the cities of the catalogue ordered by name.
    ///
    /// @param country ISO 3166-1 alpha-2 code to filter by, or blank for every country
    /// @param text    text found in the city name, or blank for every city
    /// @return the cities
    default ResponseEntity<List<CityTO>> list(String country, String text) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
