package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.GeocodeRequest;
import com.umdc.directory.api.v1.to.GeocodeResponse;
import org.springframework.http.ResponseEntity;

public interface GeocodeService {

    /// @param request the address to look up
    /// @param token   the `session-token` header; no uid in it means `401`
    default ResponseEntity<GeocodeResponse> geocode(GeocodeRequest request, String token) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_IMPLEMENTED).build();
    }
}
