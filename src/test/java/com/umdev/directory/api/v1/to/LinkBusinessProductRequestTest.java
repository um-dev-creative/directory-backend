package com.umdev.directory.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinkBusinessProductRequestTest {

    @Test
    @DisplayName("Create LinkBusinessProductRequest with valid data")
    void createLinkBusinessProductRequestWithValidData() {
        UUID businessId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LinkBusinessProductRequest request = new LinkBusinessProductRequest(businessId, productId);

        assertEquals(businessId, request.businessId());
        assertEquals(productId, request.productId());
    }

    @Test
    @DisplayName("LinkBusinessProductRequest toString method")
    void linkBusinessProductRequestToString() {
        UUID businessId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LinkBusinessProductRequest request = new LinkBusinessProductRequest(businessId, productId);

        String expectedString = "LinkBusinessProductRequest{businessId=" + businessId + ", productId=" + productId + "}";
        assertEquals(expectedString, request.toString());
    }
}
