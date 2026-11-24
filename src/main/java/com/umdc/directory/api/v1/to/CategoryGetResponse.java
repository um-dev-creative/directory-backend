package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.umdc.commons.util.DateUtil;
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
        boolean active,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String locale
) {

    /// Response without locale information (the field is then omitted from the JSON).
    public CategoryGetResponse(UUID id, String name, String description, UUID categoryParentId, LocalDateTime lastUpdate,
                               LocalDateTime createdDate, boolean active) {
        this(id, name, description, categoryParentId, lastUpdate, createdDate, active, null);
    }

    /// Copy with the name in the request locale.
    ///
    /// @param localizedName name to answer
    /// @param localeTag     request locale tag (`en`, `es`, `fr-CA`)
    /// @return the localised copy
    public CategoryGetResponse localized(String localizedName, String localeTag) {
        return new CategoryGetResponse(id, localizedName, description, categoryParentId, lastUpdate, createdDate, active, localeTag);
    }
}
