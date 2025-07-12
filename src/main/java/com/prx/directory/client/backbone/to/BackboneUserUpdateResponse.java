package com.prx.directory.client.backbone.to;

import java.time.LocalDateTime;
import java.util.UUID;

/// BackboneUserUpdateResponse record.
///
/// Represents the response received after updating a user in the backbone system. This record encapsulates all relevant user information
/// that may be returned by the backbone service following an update operation.
///
///
///     - **id**: Unique identifier of the user.
///     - **alias**: User's alias or username.
///     - **email**: User's email address.
///     - **displayName**: User's display name.
///     - **createdDate**: Date and time when the user was created.
///     - **lastUpdate**: Date and time of the last update to the user.
///     - **notificationEmail**: Indicates if email notifications are enabled for the user.
///     - **notificationSms**: Indicates if SMS notifications are enabled for the user.
///     - **privacyDataOutActive**: Indicates if the user's privacy data out is active.
///     - **active**: Indicates if the user is currently active.
///     - **personId**: Unique identifier of the associated person entity.
///     - **roleIds**: Unique identifier of the user's role.
///     - **application**: Unique identifier of the application associated with the user.
///
public record BackboneUserUpdateResponse(
    UUID id,
    String alias,
    String email,
    String displayName,
    LocalDateTime createdDate,
    LocalDateTime lastUpdate,
    Boolean notificationEmail,
    Boolean notificationSms,
    Boolean privacyDataOutActive,
    Boolean active,
    UUID personId,
    UUID roleId,
    UUID applicationId
) {}
