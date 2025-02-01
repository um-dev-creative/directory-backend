package com.prx.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.prx.commons.util.DateUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;

/**
 * A request object for creating a user.
 */
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
        String phoneNumber
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
                '}';
    }
}
