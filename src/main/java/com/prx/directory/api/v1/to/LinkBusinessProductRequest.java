package com.prx.directory.api.v1.to;

import java.util.UUID;

public record LinkBusinessProductRequest(UUID businessId, UUID productId) {

    @Override
    public String toString() {
        return "LinkBusinessProductRequest{" +
                "businessId=" + businessId +
                ", productId=" + productId +
                '}';
    }
}
