package com.umdev.directory.api.v1.to;

import com.umdev.directory.constant.FavoriteType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FavoriteCreateRequest(
        @NotNull
        FavoriteType type,
        @NotNull
        UUID itemId
) {
}
