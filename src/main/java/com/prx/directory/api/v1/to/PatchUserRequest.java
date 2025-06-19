package com.prx.directory.api.v1.to;

import java.time.LocalDate;
import java.util.UUID;

/// PatchUserRequest record.
///
/// Represents a request to patch or update a user's information in the system. This record encapsulates all fields that can be updated for a user, allowing for partial updates (patch semantics).
///
///
///     - **alias**: The user's alias or username.
///     - **password**: The user's password (if being updated).
///     - **email**: The user's email address.
///     - **firstName**: The user's first name.
///     - **middleName**: The user's middle name.
///     - **lastName**: The user's last name.
///     - **displayName**: The display name for the user.
///     - **notificationEmail**: Whether email notifications are enabled for the user.
///     - **notificationSms**: Whether SMS notifications are enabled for the user.
///     - **privacyDataOutActive**: Whether the user's privacy data out is active.
///     - **gender**: The user's gender.
///     - **birthdate**: The user's birthdate.
///     - **contact**: Contact information for the user (see [Contact]).
///     - **roleId**: The unique identifier for the user's role.
///     - **active**: Whether the user is currently active.
///
public record PatchUserRequest(
        String password,
        String email,
        String firstName,
        String middleName,
        String lastName,
        String displayName,
        Boolean notificationEmail,
        Boolean notificationSms,
        Boolean privacyDataOutActive,
        String gender,
        LocalDate birthdate,
        Contact contact,
        UUID roleId,
        Boolean active
) {

    /// Contact sub-record for person.
    ///
    /// Represents contact information for the user, such as phone number or address.
    ///
    ///
    /// @param id Unique identifier for the contact.
    /// @param content The contact content (e.g., phone number or address).
    public record Contact(
            UUID id,
            String content
    ) {
    }
}
