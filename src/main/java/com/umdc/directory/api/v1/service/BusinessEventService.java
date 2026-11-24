package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.BusinessEventRequestTO;
import com.umdc.directory.api.v1.to.BusinessEventSummaryTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;

/// Aggregated click events of a business: counting (any session) and the summary (owner only).
public interface BusinessEventService {

    /// Adds one to the daily counter of (business, campaign, type) for today (UTC). Nothing about the client is stored and an
    /// event by the owner is not counted.
    ///
    /// @return `204`; `400` invalid body or type; `404` unknown business or a campaign that is not the business's
    default ResponseEntity<Void> recordEvent(UUID businessId, BusinessEventRequestTO request, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /// @param from  first day (default: 29 days before `to`)
    /// @param to    last day (default: today, UTC)
    /// @return `200`; `400` invalid range; `401`; `403` not the owner; `404` no such business
    default ResponseEntity<BusinessEventSummaryTO> summary(UUID businessId, LocalDate from, LocalDate to, String token) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
