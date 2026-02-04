package com.umdev.directory.jpa.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessEntityTest {

    @Test
    @DisplayName("Get ID Test")
    void getIdTest() {
        BusinessEntity businessEntity = new BusinessEntity();
        UUID id = UUID.randomUUID();
        businessEntity.setId(id);
        assertEquals(id, businessEntity.getId());
    }

    @Test
    @DisplayName("Get Name Test")
    void getNameTest() {
        BusinessEntity businessEntity = new BusinessEntity();
        String name = "Test Business";
        businessEntity.setName(name);
        assertEquals(name, businessEntity.getName());
    }

    @Test
    @DisplayName("Get Description Test")
    void getDescriptionTest() {
        BusinessEntity businessEntity = new BusinessEntity();
        String description = "Test Description";
        businessEntity.setDescription(description);
        assertEquals(description, businessEntity.getDescription());
    }

    @Test
    @DisplayName("Get Created Date Test")
    void getCreatedDateTest() {
        BusinessEntity businessEntity = new BusinessEntity();
        LocalDateTime createdDate = LocalDateTime.now();
        businessEntity.setCreatedDate(createdDate);
        assertEquals(createdDate, businessEntity.getCreatedDate());
    }

    @Test
    @DisplayName("Get Last Update Test")
    void getLastUpdateTest() {
        BusinessEntity businessEntity = new BusinessEntity();
        LocalDateTime lastUpdate = LocalDateTime.now();
        businessEntity.setLastUpdate(lastUpdate);
        assertEquals(lastUpdate, businessEntity.getLastUpdate());
    }

    @Test
    @DisplayName("Get Category Foreign Key Test")
    void getCategoryFkTest() {
        BusinessEntity businessEntity = new BusinessEntity();
        CategoryEntity categoryEntity = new CategoryEntity();
        businessEntity.setCategoryFk(categoryEntity);
        assertEquals(categoryEntity, businessEntity.getCategoryFk());
    }

    @Test
    @DisplayName("Get User Foreign Key Test")
    void getUserFkTest() {
        BusinessEntity businessEntity = new BusinessEntity();
        UserEntity userEntity = new UserEntity();
        businessEntity.setUserFk(userEntity);
        assertEquals(userEntity, businessEntity.getUserFk());
    }
}
