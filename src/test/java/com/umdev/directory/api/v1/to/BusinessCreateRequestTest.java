package com.umdev.directory.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessCreateRequestTest {

    @Test
    @DisplayName("Create BusinessCreateRequest with valid data")
    void createBusinessCreateRequestWithValidData() {
        UUID categoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BusinessCreateRequest request = new BusinessCreateRequest(
                "Business Name",
                "Business Description",
                categoryId,
                userId,
                "email@example.com",
                "customer@example.com",
                "order@example.com",
                "http://example.com"
        );

        assertEquals("Business Name", request.name());
        assertEquals("Business Description", request.description());
        assertEquals(categoryId, request.categoryId());
        assertEquals(userId, request.userId());
        assertEquals("email@example.com", request.email());
        assertEquals("customer@example.com", request.customerServiceEmail());
        assertEquals("order@example.com", request.orderManagementEmail());
        assertEquals("http://example.com", request.website());
    }
}
