package com.prx.directory.client.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.general.pojo.Application;
import com.prx.commons.general.pojo.Person;
import com.prx.commons.general.pojo.Role;
import com.prx.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/// Represents the response object for a user get request.
/// Contains the user's ID, alias, password, created date, last update, active status, person, roles, and applications.
public record BackboneUserGetResponse(
        UUID id,
        String alias,
        String password,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime createdDate,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime lastUpdate,
        boolean active,
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
                ", createdDate=" + createdDate +
                ", lastUpdate=" + lastUpdate +
                ", active=" + active +
                ", person=" + person +
                ", roles=" + roles +
                ", applications=" + applications +
                '}';
    }
}
