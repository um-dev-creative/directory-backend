package com.umdev.directory.api.v1.to;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/// Request object for creating a business.
public record BusinessCreateRequest(
        @NotNull @NotEmpty
        String name,
        @NotNull
        String description,
        @NotNull @NotEmpty
        UUID categoryId,
        @NotNull @NotEmpty
        UUID userId,
        @NotNull @NotEmpty @Email
        String email,
        @Email
        String customerServiceEmail,
        @Email
        String orderManagementEmail,
        String website
) {

    ///  Returns a string representation of the object.
    @Override
    public String toString() {
        return "BusinessCreateRequest{" +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", categoryId=" + categoryId +
                ", userId=" + userId +
                ", email='" + email + '\'' +
                ", customerServiceEmail='" + customerServiceEmail + '\'' +
                ", orderManagementEmail='" + orderManagementEmail + '\'' +
                ", website='" + website + '\'' +
                '}';
    }
}
