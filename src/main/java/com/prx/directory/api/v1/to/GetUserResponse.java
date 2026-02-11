package com.prx.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Represents the response object for a user get operation.
  */
public record GetUserResponse(
        UUID id,
        String alias,
        String email,
        String firstName,
        String middleName,
        String lastName,
        String displayName,
        String profileImageRef,
        UUID phoneId,
        String phoneNumber,
        Set<UUID> businessIds,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE)
        LocalDate dateOfBirth,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime createdAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME_T)
        LocalDateTime updatedAt,
        boolean notificationEmail,
        boolean notificationSms,
        boolean privacyDataOutActive,
        boolean status,
        UUID roleId,
        UUID applicationId) {

    /**
     * String representation of the UseGetResponse object.
      */
    @Override
    public String toString() {
        return "UseGetResponse{" +
                "id=" + id +
                ", alias='" + alias + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", profileImageRef='" + profileImageRef + '\'' +
                ", phoneId=" + phoneId +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", notificationEmail=" + notificationEmail +
                ", notificationSms=" + notificationSms +
                ", privacyDataOutActive=" + privacyDataOutActive +
                ", status='" + status + '\'' +
                ", roleIds=" + roleId +
                ", applicationId=" + applicationId +
                '}';
    }
}
