package com.umdev.directory.api.v1.to;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/// Request object for updating a business.
public record BusinessUpdateRequest(
        UUID id,
        @Size(max = 255, message = "name must not exceed 255 characters")
        String name,
        @Size(max = 1500, message = "description must not exceed 1500 characters")
        String description,
        UUID categoryId,
        @Email(message = "email must be valid")
        String email,
        @Email(message = "customerServiceEmail must be valid")
        String customerServiceEmail,
        @Email(message = "orderManagementEmail must be valid")
        String orderManagementEmail,
        String website
) {
}
