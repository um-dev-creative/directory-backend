package com.prx.directory.client.to;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public record BackboneUserCreateResponse(
        /// The unique identifier of the user.
        UUID id,
        /// The alias of the user.
        String alias,
        /// The email for the user.
        String email,
        /// The date and time when the user was created.
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdDate,
        /// The date and time when the user was last updated.
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime lastUpdate,
        /// The active status of the user.
        boolean active,
        /// The unique identifier of the associated person.
        UUID personId,
        /// The unique identifier of the associated role.
        UUID roleId,
        /// The unique identifier of the associated application.
        UUID applicationId) {
}
