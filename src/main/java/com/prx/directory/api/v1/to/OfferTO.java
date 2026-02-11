package com.prx.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Minimal Offer/Campaign DTO used for favorites responses.
 */
public record OfferTO(
        UUID id,
        String name,
        String description,
        UUID businessId,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_MIL)
        LocalDateTime startDate,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_MIL)
        LocalDateTime endDate,
        BigDecimal discount,
        boolean active
) {
}
