package com.prx.directory.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BusinessCreateResponseTest {

    @Test
    @DisplayName("Create BusinessCreateResponse with valid data")
    void createBusinessCreateResponseWithValidData() {
        UUID id = UUID.randomUUID();
        String businessName = "Test Business";
        LocalDateTime createdDate = LocalDateTime.now();
        LocalDateTime updatedDate = LocalDateTime.now();

        BusinessCreateResponse response = new BusinessCreateResponse(id, businessName, createdDate, updatedDate);

        assertEquals(id, response.id());
        assertEquals(businessName, response.businessName());
        assertEquals(createdDate, response.createdDate());
        assertEquals(updatedDate, response.updatedDate());
    }

    @Test
    @DisplayName("Create BusinessCreateResponse with null id")
    void createBusinessCreateResponseWithNullId() {
        String businessName = "Test Business";
        LocalDateTime createdDate = LocalDateTime.now();
        LocalDateTime updatedDate = LocalDateTime.now();

        BusinessCreateResponse response = new BusinessCreateResponse(null, businessName, createdDate, updatedDate);

        assertNotNull(response);
        assertEquals(businessName, response.businessName());
        assertEquals(createdDate, response.createdDate());
        assertEquals(updatedDate, response.updatedDate());
    }

    @Test
    @DisplayName("Create BusinessCreateResponse with null business name")
    void createBusinessCreateResponseWithNullBusinessName() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdDate = LocalDateTime.now();
        LocalDateTime updatedDate = LocalDateTime.now();

        BusinessCreateResponse response = new BusinessCreateResponse(id, null, createdDate, updatedDate);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(createdDate, response.createdDate());
        assertEquals(updatedDate, response.updatedDate());
    }

    @Test
    @DisplayName("Create BusinessCreateResponse with null createdAt date")
    void createBusinessCreateResponseWithNullCreatedDate() {
        UUID id = UUID.randomUUID();
        String businessName = "Test Business";
        LocalDateTime updatedDate = LocalDateTime.now();

        BusinessCreateResponse response = new BusinessCreateResponse(id, businessName, null, updatedDate);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(businessName, response.businessName());
        assertEquals(updatedDate, response.updatedDate());
    }

    @Test
    @DisplayName("Create BusinessCreateResponse with null lastUpdate date")
    void createBusinessCreateResponseWithNullUpdatedDate() {
        UUID id = UUID.randomUUID();
        String businessName = "Test Business";
        LocalDateTime createdDate = LocalDateTime.now();

        BusinessCreateResponse response = new BusinessCreateResponse(id, businessName, createdDate, null);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(businessName, response.businessName());
        assertEquals(createdDate, response.createdDate());
    }
}
