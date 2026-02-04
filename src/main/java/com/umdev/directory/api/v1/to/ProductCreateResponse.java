package com.umdev.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.umdev.commons.util.DateUtil;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductCreateResponse(
        UUID id,
        String name,
        String description,
        @NotNull @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_MIL)
        LocalDateTime createdDate,
        @NotNull @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_MIL)
        LocalDateTime lastUpdate,
        UUID categoryId
) {

    @Override
    public String toString() {
        return "ProductCreateResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdDate=" + createdDate +
                ", lastUpdate=" + lastUpdate +
                ", categoryId=" + categoryId +
                '}';
    }
}
