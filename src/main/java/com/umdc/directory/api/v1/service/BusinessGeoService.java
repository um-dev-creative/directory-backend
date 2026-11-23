package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.BusinessMapResponse;
import com.umdc.directory.api.v1.to.BusinessNearbyResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/// Geographic search of businesses: nearby (list) and map (GeoJSON). Every parameter arrives as text and is validated by the service.
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public interface BusinessGeoService {

    /// @return a page of locations inside the radius with their distance; `400` (reason in `message-error`) for invalid parameters
    default ResponseEntity<BusinessNearbyResponse> nearby(String lat, String lng, String radiusM, String categoryId, String cityId,
                                                          String text, String sort, String page, String perPage) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @return a GeoJSON FeatureCollection of points or clusters; `400` (reason in `message-error`) for invalid parameters
    default ResponseEntity<BusinessMapResponse> map(String bbox, String zoom, String categoryId, String cityId, String text, String limit) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
