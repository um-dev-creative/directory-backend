package com.prx.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CategoryGetResponse(
        UUID id,
        String name,
        String description,
        UUID categoryParentId,
        @NotNull @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime lastUpdate,
        @NotNull @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime createdDate,
        boolean active
) {
}
