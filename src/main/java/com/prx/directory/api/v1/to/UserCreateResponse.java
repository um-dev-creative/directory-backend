package com.prx.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/// UserCreateResponse record.
///
/// Represents the response object returned after successfully creating a user. This record contains all relevant user information as stored in the system after creation.
///
///
///     - **id**: The unique identifier for the user.
///     - **alias**: The user's alias or username.
///     - **email**: The user's email address.
///     - **createdDate**: The date and time when the user was createdAt.
///     - **lastUpdate**: The date and time when the user was last lastUpdate.
///     - **active**: Indicates if the user is currently active.
///     - **personId**: The unique identifier of the associated person entity.
///     - **roleIds**: The unique identifier of the user's role.
///     - **applicationId**: The unique identifier of the application associated with the user.
///     - **displayName**: The display name for the user.
///     - **notificationSms**: Indicates if SMS notifications are enabled for the user.
///     - **notificationEmail**: Indicates if email notifications are enabled for the user.
///     - **privacyDataOutActive**: Indicates if the user's privacy data out is active.
///
public record UserCreateResponse(
        UUID id,
        String alias,
        String email,
        @NotNull @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_MIL)
        LocalDateTime createdDate,
        @NotNull @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_MIL)
        LocalDateTime lastUpdate,
        boolean active,
        UUID personId,
        UUID roleId,
        UUID applicationId,
        String displayName,
        Boolean notificationSms,
        Boolean notificationEmail,
        Boolean privacyDataOutActive) {

    @Override
    public String toString() {
        return "UserCreateResponse{" +
                "id=" + id +
                ", alias='" + alias + '\'' +
                ", email='" + email + '\'' +
                ", createdDate=" + createdDate +
                ", lastUpdate=" + lastUpdate +
                ", active=" + active +
                ", personId=" + personId +
                ", roleIds=" + roleId +
                ", applicationId=" + applicationId +
                ", displayName='" + displayName + '\'' +
                ", notificationSms=" + notificationSms +
                ", notificationEmail=" + notificationEmail +
                ", privacyDataOutActive=" + privacyDataOutActive +
                '}';
    }
}
