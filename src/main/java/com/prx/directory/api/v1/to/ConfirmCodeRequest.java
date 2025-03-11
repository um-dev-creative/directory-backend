package com.prx.directory.api.v1.to;

import java.util.UUID;

public record ConfirmCodeRequest(UUID userId, String verificationCode) {
    @Override
    public String toString() {
        return "ConfirmCodeRequest{" +
                "userId=" + userId +
                ", verificationCode='" + verificationCode + '\'' +
                '}';
    }
}
