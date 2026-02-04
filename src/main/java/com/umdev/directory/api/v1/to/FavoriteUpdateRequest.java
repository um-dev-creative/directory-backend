package com.umdev.directory.api.v1.to;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for updating a user favorite.
 * Only the 'active' field can be updated (for soft-delete support).
 */
public record FavoriteUpdateRequest(
        @NotNull
        UUID id,
        Boolean active
) {
}
