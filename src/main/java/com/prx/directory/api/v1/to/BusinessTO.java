package com.prx.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;


public record BusinessTO(
        UUID id,
        String name,
        String description,
        UUID userId,
        UUID categoryId,
        String email,
        String customerServiceEmail,
        String orderManagementEmail,
        String website,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdDate,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedDate,
        boolean verified,
        UUID timezoneId
) {

    @Override
    public String toString() {
        return "BusinessTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", userId=" + userId +
                ", categoryId=" + categoryId +
                ", email='" + email + '\'' +
                ", customerServiceEmail='" + customerServiceEmail + '\'' +
                ", orderManagementEmail='" + orderManagementEmail + '\'' +
                ", website='" + website + '\'' +
                ", createdDate=" + createdDate +
                ", updatedDate=" + updatedDate +
                ", verified=" + verified +
                ", timezoneId=" + timezoneId +
                '}';
    }
}
