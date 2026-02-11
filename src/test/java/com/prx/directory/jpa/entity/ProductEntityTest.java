package com.prx.directory.jpa.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductEntityTest {

    @Test
    @DisplayName("ProductEntity: getId returns assigned id")
    void getId() {
        ProductEntity productEntity = new ProductEntity();
        UUID id = UUID.randomUUID();
        productEntity.setId(id);
        assertEquals(id, productEntity.getId());
    }

    @Test
    @DisplayName("ProductEntity: getName returns assigned name")
    void getName() {
        ProductEntity productEntity = new ProductEntity();
        String name = "Test Product";
        productEntity.setName(name);
        assertEquals(name, productEntity.getName());
    }

    @Test
    @DisplayName("ProductEntity: getDescription returns assigned description")
    void getDescription() {
        ProductEntity productEntity = new ProductEntity();
        String description = "Test Description";
        productEntity.setDescription(description);
        assertEquals(description, productEntity.getDescription());
    }

    @Test
    @DisplayName("ProductEntity: getCreatedDate returns set date")
    void getCreateDate() {
        ProductEntity productEntity = new ProductEntity();
        LocalDateTime createDate = LocalDateTime.now();
        productEntity.setCreatedDate(createDate);
        assertEquals(createDate, productEntity.getCreatedDate());
    }

    @Test
    @DisplayName("ProductEntity: getLastUpdate returns set date")
    void getLastDate() {
        ProductEntity productEntity = new ProductEntity();
        LocalDateTime lastDate = LocalDateTime.now();
        productEntity.setLastUpdate(lastDate);
        assertEquals(lastDate, productEntity.getLastUpdate());
    }

    @Test
    @DisplayName("ProductEntity: getActive returns set active flag")
    void getActive() {
        ProductEntity productEntity = new ProductEntity();
        productEntity.setActive(true);
        assertTrue(productEntity.getActive());
    }

    @Test
    @DisplayName("ProductEntity: getCategoryFk returns assigned category")
    void getCategoryFk() {
        ProductEntity productEntity = new ProductEntity();
        CategoryEntity categoryEntity = new CategoryEntity();
        productEntity.setCategoryFk(categoryEntity);
        assertEquals(categoryEntity, productEntity.getCategoryFk());
    }

}
