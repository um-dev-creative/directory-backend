package com.umdev.directory.api.v1.to;

/**
 * Response object indicating whether the verification code check is completed.
 */
public record CheckVerificationCodeResponse(Boolean completed) {

    @Override
    public String toString() {
        return "CheckVerificationCodeResponse{" +
                "completed=" + completed +
                '}';
    }
}
