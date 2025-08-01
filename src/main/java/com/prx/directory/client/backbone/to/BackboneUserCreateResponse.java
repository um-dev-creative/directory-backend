package com.prx.directory.client.backbone.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;

/// BackboneUserCreateResponse record.
///
/// Represents the response received after creating a user in the backbone system. This record contains all relevant user information as returned by the backbone service after a successful creation operation.
///
///
///     - **id**: The unique identifier of the user.
///     - **alias**: The alias of the user.
///     - **email**: The email for the user.
///     - **createdDate**: The date and time when the user was createdAt.
///     - **lastUpdate**: The date and time when the user was last lastUpdate.
///     - **active**: The active status of the user.
///     - **personId**: The unique identifier of the associated person.
///     - **roleIds**: The unique identifier of the associated role.
///     - **applicationId**: The unique identifier of the associated application.
///     - **displayName**: The display name of the user.
///     - **notificationSms**: The notification SMS preference of the user.
///     - **notificationEmail**: The notification email preference of the user.
///     - **privacyDataOutActive**: The privacy data out active status of the user.
///
public record BackboneUserCreateResponse(
        /// The unique identifier of the user.
        UUID id,
        /// The alias of the user.
        String alias,
        /// The email for the user.
        String email,
        /// The date and time when the user was createdAt.
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdDate,
        /// The date and time when the user was last lastUpdate.
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime lastUpdate,
        /// The active status of the user.
        boolean active,
        /// The unique identifier of the associated person.
        UUID personId,
        /// The unique identifier of the associated role.
        UUID roleId,
        /// The unique identifier of the associated application.
        UUID applicationId,
        /// The display name of the user.
        String displayName,
        /// The notification SMS preference of the user.
        Boolean notificationSms,
        /// The notification email preference of the user.
        Boolean notificationEmail,
        /// The privacy data out active status of the user.
        Boolean privacyDataOutActive
) {}
