package com.prx.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.prx.commons.util.DateUtil;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for updating an existing campaign. All fields are optional to support partial updates (PATCH semantics).
 * Validations apply only to fields that are provided.
 */
public record CampaignUpdateRequest(
        @Size(max = 120, message = "title must not exceed 120 characters")
        String title,
        @Size(max = 1200, message = "description must not exceed 1200 characters")
        String description,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime startDate,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime endDate,
        UUID categoryId,
        UUID businessId,
        Boolean active,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime lastUpdate
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
