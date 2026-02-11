package com.prx.directory.api.v1.to;

import com.prx.directory.constant.FavoriteType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FavoriteCreateRequest(
        @NotNull
        FavoriteType type,
        @NotNull
        UUID itemId
) {
}
