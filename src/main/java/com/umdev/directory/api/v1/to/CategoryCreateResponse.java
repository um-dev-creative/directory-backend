package com.umdev.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.umdev.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoryCreateResponse(
        UUID id,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_MIL)
        LocalDateTime createdDate
) {

    @Override
    public String toString() {
        return "CategoryCreateResponse{" +
                "id=" + id +
                ", createdDate=" + createdDate +
                '}';
    }
}

