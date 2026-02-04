package com.umdev.directory.jpa.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CategoryEntityTest {

    @Test
    @DisplayName("Get ID")
    void getId() {
        CategoryEntity categoryEntity = new CategoryEntity();
        UUID id = UUID.randomUUID();
        categoryEntity.setId(id);
        assertEquals(id, categoryEntity.getId());
    }

    @Test
    @DisplayName("Set ID to Null")
    void setIdToNull() {
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setId(null);
        assertNull(categoryEntity.getId());
    }

    @Test
    @DisplayName("Get Name")
    void getName() {
        CategoryEntity categoryEntity = new CategoryEntity();
        String name = "Test Category";
        categoryEntity.setName(name);
        assertEquals(name, categoryEntity.getName());
    }

    @Test
    @DisplayName("Set Name to Null")
    void setNameToNull() {
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setName(null);
        assertNull(categoryEntity.getName());
    }

    @Test
    @DisplayName("Get Description")
    void getDescription() {
        CategoryEntity categoryEntity = new CategoryEntity();
        String description = "Test Description";
        categoryEntity.setDescription(description);
        assertEquals(description, categoryEntity.getDescription());
    }

    @Test
    @DisplayName("Set Description to Null")
    void setDescriptionToNull() {
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setDescription(null);
        assertNull(categoryEntity.getDescription());
    }

    @Test
    @DisplayName("Get Create Date")
    void getCreateDate() {
        CategoryEntity categoryEntity = new CategoryEntity();
        LocalDateTime createDate = LocalDateTime.now();
        categoryEntity.setCreatedDate(createDate);
        assertEquals(createDate, categoryEntity.getCreatedDate());
    }

    @Test
    @DisplayName("Set Create Date to Null")
    void setCreateDateToNull() {
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setCreatedDate(null);
        assertNull(categoryEntity.getCreatedDate());
    }

    @Test
    @DisplayName("Get Last Update")
    void getLastUpdate() {
        CategoryEntity categoryEntity = new CategoryEntity();
        LocalDateTime lastUpdate = LocalDateTime.now();
        categoryEntity.setLastUpdate(lastUpdate);
        assertEquals(lastUpdate, categoryEntity.getLastUpdate());
    }

    @Test
    @DisplayName("Set Last Update to Null")
    void setLastUpdateToNull() {
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setLastUpdate(null);
        assertNull(categoryEntity.getLastUpdate());
    }

    @Test
    @DisplayName("Get Active Status")
    void getActive() {
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setActive(true);
        assertTrue(categoryEntity.getActive());
    }

    @Test
    @DisplayName("Set Active Status to Null")
    void setActiveToNull() {
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setActive(null);
        assertNull(categoryEntity.getActive());
    }

    @Test
    @DisplayName("Get Category Parent Foreign Key")
    void getCategoryParentFk() {
        CategoryEntity categoryEntity = new CategoryEntity();
        CategoryEntity parentCategory = new CategoryEntity();
        categoryEntity.setCategoryParentFk(parentCategory);
        assertEquals(parentCategory, categoryEntity.getCategoryParentFk());
    }

    @Test
    @DisplayName("Set Category Parent Foreign Key to Null")
    void setCategoryParentFkToNull() {
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setCategoryParentFk(null);
        assertNull(categoryEntity.getCategoryParentFk());
    }
}
