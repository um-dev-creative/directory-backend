package com.umdc.directory.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CategoryGetResponseTest {

    @Test
    @DisplayName("Create CategoryGetResponse with valid data")
    void createCategoryGetResponseWithValidData() {
        UUID id = UUID.randomUUID();
        String name = "Category Name";
        String description = "Category Description";
        UUID categoryParentId = UUID.randomUUID();
        LocalDateTime lastUpdate = LocalDateTime.now();
        LocalDateTime createdDate = LocalDateTime.now();
        boolean active = true;

        CategoryGetResponse response = new CategoryGetResponse(id, name, description, categoryParentId, lastUpdate, createdDate, active);

        assertEquals(id, response.id());
        assertEquals(name, response.name());
        assertEquals(description, response.description());
        assertEquals(categoryParentId, response.categoryParentId());
        assertEquals(lastUpdate, response.lastUpdate());
        assertEquals(createdDate, response.createdDate());
        assertEquals(active, response.active());
    }

    @Test
    @DisplayName("Create CategoryGetResponse with null id")
    void createCategoryGetResponseWithNullId() {
        String name = "Category Name";
        String description = "Category Description";
        UUID categoryParentId = UUID.randomUUID();
        LocalDateTime lastUpdate = LocalDateTime.now();
        LocalDateTime createdDate = LocalDateTime.now();
        boolean active = true;

        CategoryGetResponse response = new CategoryGetResponse(null, name, description, categoryParentId, lastUpdate, createdDate, active);

        assertNotNull(response);
        assertEquals(name, response.name());
        assertEquals(description, response.description());
        assertEquals(categoryParentId, response.categoryParentId());
        assertEquals(lastUpdate, response.lastUpdate());
        assertEquals(createdDate, response.createdDate());
        assertEquals(active, response.active());
    }

    @Test
    @DisplayName("Create CategoryGetResponse with null name")
    void createCategoryGetResponseWithNullName() {
        UUID id = UUID.randomUUID();
        String description = "Category Description";
        UUID categoryParentId = UUID.randomUUID();
        LocalDateTime lastUpdate = LocalDateTime.now();
        LocalDateTime createdDate = LocalDateTime.now();
        boolean active = true;

        CategoryGetResponse response = new CategoryGetResponse(id, null, description, categoryParentId, lastUpdate, createdDate, active);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(description, response.description());
        assertEquals(categoryParentId, response.categoryParentId());
        assertEquals(lastUpdate, response.lastUpdate());
        assertEquals(createdDate, response.createdDate());
        assertEquals(active, response.active());
    }

    @Test
    @DisplayName("Create CategoryGetResponse with null description")
    void createCategoryGetResponseWithNullDescription() {
        UUID id = UUID.randomUUID();
        String name = "Category Name";
        UUID categoryParentId = UUID.randomUUID();
        LocalDateTime lastUpdate = LocalDateTime.now();
        LocalDateTime createdDate = LocalDateTime.now();
        boolean active = true;

        CategoryGetResponse response = new CategoryGetResponse(id, name, null, categoryParentId, lastUpdate, createdDate, active);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(name, response.name());
        assertEquals(categoryParentId, response.categoryParentId());
        assertEquals(lastUpdate, response.lastUpdate());
        assertEquals(createdDate, response.createdDate());
        assertEquals(active, response.active());
    }

    @Test
    @DisplayName("Create CategoryGetResponse with null categoryParentId")
    void createCategoryGetResponseWithNullCategoryParentId() {
        UUID id = UUID.randomUUID();
        String name = "Category Name";
        String description = "Category Description";
        LocalDateTime lastUpdate = LocalDateTime.now();
        LocalDateTime createdDate = LocalDateTime.now();
        boolean active = true;

        CategoryGetResponse response = new CategoryGetResponse(id, name, description, null, lastUpdate, createdDate, active);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(name, response.name());
        assertEquals(description, response.description());
        assertEquals(lastUpdate, response.lastUpdate());
        assertEquals(createdDate, response.createdDate());
        assertEquals(active, response.active());
    }


    @org.junit.jupiter.api.Test
    void localizedCopyKeepsOtherFieldsAndSetsLocale() {
        var id = java.util.UUID.randomUUID();
        var base = new CategoryGetResponse(id, "Food", "d", null, null, null, true);
        org.junit.jupiter.api.Assertions.assertNull(base.locale());
        var copy = base.localized("Comida", "es");
        org.junit.jupiter.api.Assertions.assertEquals("Comida", copy.name());
        org.junit.jupiter.api.Assertions.assertEquals("es", copy.locale());
        org.junit.jupiter.api.Assertions.assertEquals(id, copy.id());
        org.junit.jupiter.api.Assertions.assertEquals("d", copy.description());
        org.junit.jupiter.api.Assertions.assertTrue(copy.active());
    }
}
