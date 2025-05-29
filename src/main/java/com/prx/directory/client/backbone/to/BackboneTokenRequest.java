package com.prx.directory.client.backbone.to;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BackboneTokenRequest(
        @NotEmpty
        String email,
        @NotEmpty
        String password,
        @NotNull
        UUID applicationId
) {
}
