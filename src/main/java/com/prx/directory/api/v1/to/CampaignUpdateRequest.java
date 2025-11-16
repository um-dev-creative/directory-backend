package com.prx.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for updating an existing campaign. All fields are optional to support partial updates (PATCH semantics).
 * Validations apply only to fields that are provided.
 */
public record CampaignUpdateRequest(
        @Size(max = 120, message = "name must not exceed 120 characters")
        String name,
        @Size(max = 1200, message = "description must not exceed 1200 characters")
        String description,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant startDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant endDate,
        UUID categoryId,
        UUID businessId,
        Boolean active,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant lastUpdate
) {
    @AssertTrue(message = "startDate must be before or equal to endDate")
    @JsonIgnore
    public boolean isStartBeforeOrEqualEnd() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.isAfter(endDate);
    }
}
