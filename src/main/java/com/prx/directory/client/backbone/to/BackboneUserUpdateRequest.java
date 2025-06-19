package com.prx.directory.client.backbone.to;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/// BackboneUserUpdateRequest record.
///
/// Represents a request to update a user in the backbone system. This record encapsulates all fields that can be updated for a user, including personal, authentication, and notification details.
///
///
///     - **displayName**: The display name for the user.
///     - **password**: The password for the user (if being updated).
///     - **notificationEmail**: Indicates if email notifications are enabled for the user.
///     - **notificationSms**: Indicates if SMS notifications are enabled for the user.
///     - **privacyDataOutActive**: Indicates if the user's privacy data out is active.
///     - **firstName**: The user's first name.
///     - **middleName**: The user's middle name.
///     - **lastName**: The user's last name.
///     - **gender**: The user's gender.
///     - **birthdate**: The user's birthdate.
///     - **active**: Indicates if the user is currently active.
///     - **contacts**: List of contact information for the user (see [Contact]).
///
///
/// The [Contact] sub-record represents contact information for the user, and the [ContactType] sub-record represents the type of contact.
///
public record BackboneUserUpdateRequest(
        String displayName,
        String password,
        Boolean notificationEmail,
        Boolean notificationSms,
        Boolean privacyDataOutActive,
        String firstName,
        String middleName,
        String lastName,
        String gender,
        LocalDate birthdate,
        Boolean active,
        List<Contact> contacts
) {
    /// Contact sub-record for person.
    ///
    /// Represents contact information for the user, such as phone number or address.
    ///
    ///
    /// @param id Unique identifier for the contact.
    /// @param content The contact content (e.g., phone number or address).
    /// @param contactType The type of contact (see [ContactType]).
    /// @param active Indicates if the contact is currently active.
    public record Contact(
            UUID id,
            String content,
            ContactType contactType,
            Boolean active
    ) {}

    /**
     * ContactType sub-record for contact.
     * <p>
     * Represents the type of contact (e.g., phone, email).
     * </p>
     *
     * @param id Unique identifier for the contact type.
     */
    public record ContactType(
            UUID id
    ) {}
}
