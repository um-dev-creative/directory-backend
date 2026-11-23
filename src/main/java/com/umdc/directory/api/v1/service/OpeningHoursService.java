package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.OpeningHoursTO;
import com.umdc.directory.api.v1.to.OpeningStatusTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

/// Opening hours of a location: read and status (public), replace (owner only).
public interface OpeningHoursService {

    /// @return `200` with the slots (empty when none were set); `404` when the business or the location does not exist
    default ResponseEntity<OpeningHoursTO> get(UUID businessId, UUID locationId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// Replaces the whole schedule.
    ///
    /// @return `200` with the stored hours; `400` invalid body (reason in `message-error`); `401`; `403` not the owner; `404`
    default ResponseEntity<OpeningHoursTO> replace(UUID businessId, UUID locationId, OpeningHoursTO body, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @param at optional ISO-8601 instant; now when null or blank
    /// @return `200` with the status; `400` invalid `at`; `404` when the business or the location does not exist
    default ResponseEntity<OpeningStatusTO> status(UUID businessId, UUID locationId, String at) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
