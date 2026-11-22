package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.LocationCreateRequest;
import com.umdc.directory.api.v1.to.LocationTO;
import com.umdc.directory.api.v1.to.LocationUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

/// Operations on the locations (sedes) of a business. Reading is open to any session; changing is reserved for the owner of the business.
public interface LocationService {

    /// @param businessId the business
    /// @return its locations, the main one first; `404` when the business does not exist
    default ResponseEntity<List<LocationTO>> list(UUID businessId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @param businessId the business
    /// @param request    the new location
    /// @param token      the session token of the caller (its `uid` must be the owner)
    /// @return `201` with the location; `400` invalid body or unknown city; `401`; `403` not the owner; `404` no such business
    default ResponseEntity<LocationTO> create(UUID businessId, LocationCreateRequest request, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @param businessId the business
    /// @param locationId the location
    /// @param request    the fields to change
    /// @param token      the session token of the caller (its `uid` must be the owner)
    /// @return `200` with the location; `400`, `401`, `403`, `404` as in {@link #create}
    default ResponseEntity<LocationTO> update(UUID businessId, UUID locationId, LocationUpdateRequest request, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @param businessId the business
    /// @param locationId the location
    /// @param token      the session token of the caller (its `uid` must be the owner)
    /// @return `204`; `401`, `403`, `404` as in {@link #create}
    default ResponseEntity<Void> delete(UUID businessId, UUID locationId, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
