package com.umdev.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.umdev.commons.util.DateUtil;

import java.time.LocalDateTime;

/**
 * Minimal response for PATCH /businesses/{id} following project TO conventions.
 */
public record BusinessUpdateResponse(
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime updatedDate
) {
}

