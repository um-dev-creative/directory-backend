package com.umdev.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.umdev.commons.util.DateUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;

/// UserCreateRequest record.
///
/// Represents a request object for creating a new user in the system. This record encapsulates all the necessary fields required for user registration, including personal information, contact details, and notification preferences.
///
///
///     - **password**: The user's password. Must not be blank.
///     - **email**: The user's email address. Must not be blank and must be a valid email format.
///     - **firstname**: The user's first name. Must not be blank.
///     - **lastname**: The user's last name. Must not be blank.
///     - **dateOfBirth**: The user's date of birth. Must not be empty and must follow the pattern defined in [#PATTERN_DATE].
///     - **phoneNumber**: The user's phoneNumber number. Must not be blank.
///     - **displayName**: The display name for the user (optional).
///     - **notificationSms**: Indicates if SMS notifications are enabled for the user (optional).
///     - **notificationEmail**: Indicates if email notifications are enabled for the user (optional).
///     - **privacyDataOutActive**: Indicates if the user's privacy data out is active (optional).
///
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserCreateRequest(
        @NotBlank
        String password,
        @NotBlank @Email
        String email,
        @NotBlank
        String firstname,
        @NotBlank
        String lastname,
        @NotEmpty @JsonFormat(pattern = DateUtil.PATTERN_DATE)
        LocalDate dateOfBirth,
        @NotBlank
        String phoneNumber,
        String displayName,
        Boolean notificationSms,
        Boolean notificationEmail,
        Boolean privacyDataOutActive
) {

    @Override
    public String toString() {
        return "UserCreateRequest{" +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                ", firstname='" + firstname + '\'' +
                ", lastname='" + lastname + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", displayName='" + displayName + '\'' +
                ", notificationSms=" + notificationSms +
                ", notificationEmail=" + notificationEmail +
                ", privacyDataOutActive=" + privacyDataOutActive +
                '}';
    }
}
