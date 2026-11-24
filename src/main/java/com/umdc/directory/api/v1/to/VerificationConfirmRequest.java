package com.umdc.directory.api.v1.to;

/// The verification code the owner received. The code is never printed.
public record VerificationConfirmRequest(String code) {

    @Override
    public String toString() {
        return "VerificationConfirmRequest[code=***]";
    }
}
