package com.prx.directory.client.to;

import com.prx.commons.general.pojo.Person;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BackboneUserCreateRequest(
        /// The unique identifier for the user.
        UUID id,
        /// The unique alias for the user.
        @NotNull @NotBlank
        String alias,
        /// The password for the user.
        @NotNull @NotBlank
        String password,
        /// The email for the user.
        @NotNull @NotBlank @Email
        String email,
        /// Indicates if the user is active.
        boolean active,
        /// The person identifier for the user.
        @NotNull
        Person person,
        /// The roles assigned to the user.
        @NotNull
        UUID roleId,
        /// The application identifier for the user.
        @NotNull
        UUID applicationId) {

    public BackboneUserCreateRequest(UUID id, String alias, String password, String email, Person person, UUID roleId, UUID applicationId) {
        this(id, alias, password, email, true, person, roleId, applicationId);
    }

    @Override
    public String toString() {
        return "BackboneUserCreateRequest{" +
                "id=" + id +
                ", alias='" + alias + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                ", active=" + active +
                ", person=" + person +
                ", roleId=" + roleId +
                ", applicationId=" + applicationId +
                '}';
    }
}
