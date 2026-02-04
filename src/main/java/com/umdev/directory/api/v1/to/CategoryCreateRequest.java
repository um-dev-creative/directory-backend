package com.umdev.directory.api.v1.to;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoryCreateRequest(
        @NotBlank(message = "Empty value is invalid, name is required")
        String name,
        String description,
        UUID categoryParentId,
        Boolean active
) {

    @Override
    public String toString() {
        return "CategoryCreateRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", categoryParentId=" + categoryParentId +
                ", active=" + active +
                '}';
    }
}

