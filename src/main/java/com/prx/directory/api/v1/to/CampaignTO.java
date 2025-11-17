package com.prx.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.prx.commons.util.DateUtil;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * POJO for Campaign transport. Converted to a record to avoid duplicated getter/setter code
 * (prevents PMD CPD failures) while preserving validation annotations.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CampaignTO(
        UUID id,
        @NotNull(message = "name is required")
        @Size(max = 120)
        String name,
        @Size(max = 1200)
        String description,
        @NotNull(message = "startDate is required")
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime startDate,
        @NotNull(message = "endDate is required")
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime endDate,
        UUID categoryId,
        UUID businessId,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime createdDate,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime lastUpdate,
        Boolean active
) {
    @AssertTrue(message = "startDate must be before or equal to endDate")
    @JsonIgnore
    public boolean isStartBeforeOrEqualEnd() {
        if (startDate == null || endDate == null) return true;
        return !startDate.isAfter(endDate);
    }
}
