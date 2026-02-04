package com.umdev.directory.client.backbone.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.umdev.commons.general.pojo.Application;
import com.umdev.commons.general.pojo.Person;
import com.umdev.commons.general.pojo.Role;
import com.umdev.commons.util.DateUtil;
import jakarta.validation.constraints.Email;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/// Represents the response object for a user get request.
/// Contains the user's ID, alias, password, createdAt date, last update, active status, person, roles, and applications.
public record BackboneUserGetResponse(
        UUID id,
        String alias,
        String password,
        @Email
        String email,
        String displayName,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime createdDate,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime lastUpdate,
        boolean active,
        boolean notificationEmail,
        boolean notificationSms,
        boolean privacyDataOutActive,
        Person person,
        List<Role> roles,
        List<Application> applications
) {

    ///  String representation of the BackboneUserGetResponse.
    @Override
    public String toString() {
        return "BackboneUserGetResponse{" +
                "id=" + id +
                ", alias='" + alias + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", createdDate=" + createdDate +
                ", lastUpdate=" + lastUpdate +
                ", notificationEmail=" + notificationEmail +
                ", notificationSms=" + notificationSms +
                ", privacyDataOutActive=" + privacyDataOutActive +
                ", active=" + active +
                ", person=" + person +
                ", roles=" + roles +
                ", applications=" + applications +
                '}';
    }
}
