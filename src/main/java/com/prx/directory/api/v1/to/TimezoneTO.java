package com.prx.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public record TimezoneTO(
        UUID id,
        String name,
        Duration utcOffset,
        String abbreviation,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime createdAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime lastUpdate) {
}
