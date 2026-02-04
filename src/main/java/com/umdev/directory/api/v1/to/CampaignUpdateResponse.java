package com.umdev.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.umdev.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Minimal response for PATCH /campaigns/{id} following project TO conventions.
 */
public record CampaignUpdateResponse(
        UUID id,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime lastUpdate
) {
}

