package com.prx.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.UUID;

/**
 * Minimal Offer/Campaign DTO used for favorites responses.
 */
public record OfferTO(
        UUID id,
        String name,
        String description,
        UUID businessId,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant startDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant endDate,
        boolean active
) {
}

