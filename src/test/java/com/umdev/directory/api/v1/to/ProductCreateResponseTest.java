package com.umdev.directory.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductCreateResponseTest {

    @Test
    @DisplayName("Create ProductCreateResponse successfully")
    void createProductCreateResponseSuccessfully() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        ProductCreateResponse response = new ProductCreateResponse(id, "Example Product", "This is an example product description.", now, now, categoryId);

        assertEquals(id, response.id());
        assertEquals("Example Product", response.name());
        assertEquals("This is an example product description.", response.description());
        assertEquals(now, response.createdDate());
        assertEquals(now, response.lastUpdate());
        assertEquals(categoryId, response.categoryId());
    }

    @Test
    @DisplayName("Create ProductCreateResponse with null id")
    void createProductCreateResponseWithNullId() {
        UUID categoryId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        ProductCreateResponse response = new ProductCreateResponse(null, "Example Product", "This is an example product description.", now, now, categoryId);

        assertNotNull(response);
        assertEquals("Example Product", response.name());
        assertEquals("This is an example product description.", response.description());
        assertEquals(now, response.createdDate());
        assertEquals(now, response.lastUpdate());
        assertEquals(categoryId, response.categoryId());
    }

    @Test
    @DisplayName("Create ProductCreateResponse with null name")
    void createProductCreateResponseWithNullName() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        ProductCreateResponse response = new ProductCreateResponse(id, null, "This is an example product description.", now, now, categoryId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals("This is an example product description.", response.description());
        assertEquals(now, response.createdDate());
        assertEquals(now, response.lastUpdate());
        assertEquals(categoryId, response.categoryId());
    }

    @Test
    @DisplayName("Create ProductCreateResponse with null description")
    void createProductCreateResponseWithNullDescription() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        ProductCreateResponse response = new ProductCreateResponse(id, "Example Product", null, now, now, categoryId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals("Example Product", response.name());
        assertEquals(now, response.createdDate());
        assertEquals(now, response.lastUpdate());
        assertEquals(categoryId, response.categoryId());
    }

    @Test
    @DisplayName("Create ProductCreateResponse with null createdDate")
    void createProductCreateResponseWithNullCreatedDate() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        ProductCreateResponse response = new ProductCreateResponse(id, "Example Product", "This is an example product description.", null, now, categoryId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals("Example Product", response.name());
        assertEquals("This is an example product description.", response.description());
        assertEquals(now, response.lastUpdate());
        assertEquals(categoryId, response.categoryId());
    }

    @Test
    @DisplayName("Create ProductCreateResponse with null lastUpdate")
    void createProductCreateResponseWithNullLastUpdate() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        ProductCreateResponse response = new ProductCreateResponse(id, "Example Product", "This is an example product description.", now, null, categoryId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals("Example Product", response.name());
        assertEquals("This is an example product description.", response.description());
        assertEquals(now, response.createdDate());
        assertEquals(categoryId, response.categoryId());
    }

    @Test
    @DisplayName("Create ProductCreateResponse with null categoryId")
    void createProductCreateResponseWithNullCategoryId() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        ProductCreateResponse response = new ProductCreateResponse(id, "Example Product", "This is an example product description.", now, now, null);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals("Example Product", response.name());
        assertEquals("This is an example product description.", response.description());
        assertEquals(now, response.createdDate());
        assertEquals(now, response.lastUpdate());
    }
}
