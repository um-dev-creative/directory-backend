package com.umdev.directory.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LinkBusinessProductResponseTest {

    @Test
    @DisplayName("Create LinkBusinessProductResponse successfully")
    void createLinkBusinessProductResponseSuccessfully() {
        UUID businessId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();
        Boolean active = true;

        LinkBusinessProductResponse response = new LinkBusinessProductResponse(businessId, productId, createdAt, lastUpdate, active);

        assertEquals(businessId, response.businessId());
        assertEquals(productId, response.productId());
        assertEquals(createdAt, response.createdAt());
        assertEquals(lastUpdate, response.lastUpdate());
        assertEquals(active, response.active());
    }

    @Test
    @DisplayName("Create LinkBusinessProductResponse with null businessId")
    void createLinkBusinessProductResponseWithNullBusinessId() {
        UUID productId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();
        Boolean active = true;

        LinkBusinessProductResponse response = new LinkBusinessProductResponse(null, productId, createdAt, lastUpdate, active);

        assertNull(response.businessId());
        assertEquals(productId, response.productId());
        assertEquals(createdAt, response.createdAt());
        assertEquals(lastUpdate, response.lastUpdate());
        assertEquals(active, response.active());
    }

    @Test
    @DisplayName("Create LinkBusinessProductResponse with null productId")
    void createLinkBusinessProductResponseWithNullProductId() {
        UUID businessId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();
        Boolean active = true;

        LinkBusinessProductResponse response = new LinkBusinessProductResponse(businessId, null, createdAt, lastUpdate, active);

        assertEquals(businessId, response.businessId());
        assertNull(response.productId());
        assertEquals(createdAt, response.createdAt());
        assertEquals(lastUpdate, response.lastUpdate());
        assertEquals(active, response.active());
    }

    @Test
    @DisplayName("Create LinkBusinessProductResponse with null createdAt")
    void createLinkBusinessProductResponseWithNullCreatedAt() {
        UUID businessId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDateTime lastUpdate = LocalDateTime.now();
        Boolean active = true;

        LinkBusinessProductResponse response = new LinkBusinessProductResponse(businessId, productId, null, lastUpdate, active);

        assertEquals(businessId, response.businessId());
        assertEquals(productId, response.productId());
        assertNull(response.createdAt());
        assertEquals(lastUpdate, response.lastUpdate());
        assertEquals(active, response.active());
    }

    @Test
    @DisplayName("Create LinkBusinessProductResponse with null lastUpdate")
    void createLinkBusinessProductResponseWithNullLastUpdate() {
        UUID businessId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        Boolean active = true;

        LinkBusinessProductResponse response = new LinkBusinessProductResponse(businessId, productId, createdAt, null, active);

        assertEquals(businessId, response.businessId());
        assertEquals(productId, response.productId());
        assertEquals(createdAt, response.createdAt());
        assertNull(response.lastUpdate());
        assertEquals(active, response.active());
    }

    @Test
    @DisplayName("Create LinkBusinessProductResponse with null active")
    void createLinkBusinessProductResponseWithNullActive() {
        UUID businessId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();

        LinkBusinessProductResponse response = new LinkBusinessProductResponse(businessId, productId, createdAt, lastUpdate, null);

        assertEquals(businessId, response.businessId());
        assertEquals(productId, response.productId());
        assertEquals(createdAt, response.createdAt());
        assertEquals(lastUpdate, response.lastUpdate());
        assertNull(response.active());
    }
}
