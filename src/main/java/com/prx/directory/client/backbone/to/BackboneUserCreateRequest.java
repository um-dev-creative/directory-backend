package com.prx.directory.client.backbone.to;

import com.prx.commons.general.pojo.Person;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * BackboneUserCreateRequest record.
 * <p>
 * Represents a request to create a new user in the backbone system. This record encapsulates all the necessary fields required for user creation, including personal, authentication, and notification details.
 * </p>
 *
 * <ul>
 *   <li><b>id</b>: The unique identifier for the user.</li>
 *   <li><b>alias</b>: The unique alias for the user.</li>
 *   <li><b>password</b>: The password for the user.</li>
 *   <li><b>email</b>: The email for the user.</li>
 *   <li><b>active</b>: Indicates if the user is active.</li>
 *   <li><b>roleIds</b>: The roles assigned to the user.</li>
 *   <li><b>applicationId</b>: The application identifier for the user.</li>
 *   <li><b>displayName</b>: The display name for the user.</li>
 *   <li><b>notificationSms</b>: Indicates if the user wants to receive notifications via SMS.</li>
 *   <li><b>notificationEmail</b>: Indicates if the user wants to receive notifications via Email.</li>
 *   <li><b>privacyDataOutActive</b>: Indicates if the user's data can be sent outside the application.</li>
 * </ul>
 */
public record BackboneUserCreateRequest(
        @NotNull UUID id,
        @NotNull @NotBlank String alias,
        @NotNull @NotBlank String password,
        @NotNull @NotBlank @Email String email,
        boolean active,
        @NotNull Person person,
        @NotNull UUID roleId,
        @NotNull UUID applicationId,
        String displayName,
        Boolean notificationSms,
        Boolean notificationEmail,
        Boolean privacyDataOutActive
) {

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
                ", displayName='" + displayName + '\'' +
                ", notificationSms=" + notificationSms +
                ", notificationEmail=" + notificationEmail +
                ", privacyDataOutActive=" + privacyDataOutActive +
                '}';
    }
}
