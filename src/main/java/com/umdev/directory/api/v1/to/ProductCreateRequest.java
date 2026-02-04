package com.umdev.directory.api.v1.to;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProductCreateRequest(
        @NotBlank(message = "Empty value invalid,description required")
        String name,
        @NotBlank(message = "Empty value invalid,description required")
        String description,
        @NotNull
        UUID categoryId
) {

    @Override
    public String toString() {
        return "ProductCreateRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", categoryId=" + categoryId +
                '}';
    }
}
