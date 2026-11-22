package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.BusinessStatsTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

/// Views of a business: counting (any session) and the summary (owner only).
public interface BusinessStatsService {

    /// Counts one view of the business for today (UTC). Nothing about the viewer is stored, and a view by the owner is not counted.
    ///
    /// @param businessId the business that was viewed
    /// @param token      the session token of the viewer (only used to recognise the owner)
    /// @return `204`; `404` when the business does not exist
    default ResponseEntity<Void> recordView(UUID businessId, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @param businessId the business
    /// @param days       size of the window ending today, 1 to 365 (default 30)
    /// @param token      the session token of the caller (its `uid` must be the owner)
    /// @return `200` with the summary; `400` invalid `days`; `401`; `403` not the owner; `404` no such business
    default ResponseEntity<BusinessStatsTO> summary(UUID businessId, Integer days, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
