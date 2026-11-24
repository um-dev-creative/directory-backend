package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.BusinessSearchResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

/// Search of businesses by city, category and text.
public interface BusinessSearchService {

    /// @param cityId     only businesses with a location in this city
    /// @param city       only businesses with a location in the city of this name (ignored when `cityId` is given)
    /// @param categoryId only businesses of this category or of its sub-categories
    /// @param text       text found in the name or the description
    /// @param page       page number, 1-based (default 1)
    /// @param perPage    page size (default 10, max 100)
    /// @return a page of businesses with their locations; `400` for an invalid page or size
    default ResponseEntity<BusinessSearchResponse> search(UUID cityId, String city, UUID categoryId, String text, Integer page, Integer perPage) {
        return search(cityId, city, categoryId, text, page, perPage, null, null, null, null);
    }

    /// Same as the search above plus the optional `open_now` and the point of the caller.
    ///
    /// @param openNow `true` keeps only the businesses open right now (`false` or null: no filter)
    /// @param lat     latitude of the caller; sent together with `lng`, it fills `distance_m`
    /// @param lng     longitude of the caller
    /// @param sort    `name` (default) or `distance` (needs `lat` and `lng`)
    /// @return a page of businesses; `400` for an invalid page, size, coordinate or sort
    @SuppressWarnings("PMD.UseObjectForClearerAPI") // the parameters are the query string
    default ResponseEntity<BusinessSearchResponse> search(UUID cityId, String city, UUID categoryId, String text, Integer page, Integer perPage,
                                                          Boolean openNow, String lat, String lng, String sort) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
