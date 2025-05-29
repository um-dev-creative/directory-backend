package com.prx.directory.client.mercury.to;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/// Represents the request object for a verification code.
public record VerificationCodeRequest(
        @NotNull UUID applicationId,
        @NotNull UUID userId,
        @NotNull @NotEmpty @Size(min = 9, max = 9) String code) {
    @Override
    public String toString() {
        return "VerificationCodeRequest{" +
                "applicationId=" + applicationId +
                ", userId=" + userId +
                ", code='" + code + '\'' +
                '}';
    }

}
