package com.umdev.directory.api.v1.to;

import java.util.List;
import java.util.UUID;

/// PutUserRequest record.
///
/// Represents a request to update a user's information in the system. This record encapsulates all fields that can be lastUpdate for a user.
///
/// Fields:
///   - **firstName**: The user's first name.
///   - **lastName**: The user's last name.
///   - **displayName**: The display name for the user.
///   - **notificationEmail**: Whether email notifications are enabled for the user.
///   - **notificationSms**: Whether SMS notifications are enabled for the user.
///   - **privacyDataOutActive**: Whether the user's privacy data out is active.
///   - **phoneId**: The unique identifier for the user's phoneNumber.
///   - **phoneNumber**: The user's phoneNumber number.
///   - **roleIds**: The collection of unique identifiers for the user's roles.
///   - **active**: Whether the user is currently active.
///
public record PutUserRequest(
        String firstName,
        String lastName,
        String displayName,
        Boolean notificationEmail,
        Boolean notificationSms,
        Boolean privacyDataOutActive,
        UUID phoneId,
        String phoneNumber,
        List<UUID> roleIds,
        Boolean active
) {
}
