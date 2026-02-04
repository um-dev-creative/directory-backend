package com.umdev.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.umdev.commons.util.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight DTO used for campaign list summaries returned by the collection endpoint.
 */
public record CampaignResumeTO(
        UUID id,
        String title,
        String description,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime endDate,
        String categoryName,
        BigDecimal discount,
        String status
) {}

