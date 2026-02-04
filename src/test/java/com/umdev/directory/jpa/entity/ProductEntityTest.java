package com.umdev.directory.jpa.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductEntityTest {

    @Test
    void getId() {
        ProductEntity productEntity = new ProductEntity();
        UUID id = UUID.randomUUID();
        productEntity.setId(id);
        assertEquals(id, productEntity.getId());
    }

    @Test
    void getName() {
        ProductEntity productEntity = new ProductEntity();
        String name = "Test Product";
        productEntity.setName(name);
        assertEquals(name, productEntity.getName());
    }

    @Test
    void getDescription() {
        ProductEntity productEntity = new ProductEntity();
        String description = "Test Description";
        productEntity.setDescription(description);
        assertEquals(description, productEntity.getDescription());
    }

    @Test
    void getCreateDate() {
        ProductEntity productEntity = new ProductEntity();
        LocalDateTime createDate = LocalDateTime.now();
        productEntity.setCreatedDate(createDate);
        assertEquals(createDate, productEntity.getCreatedDate());
    }

    @Test
    void getLastDate() {
        ProductEntity productEntity = new ProductEntity();
        LocalDateTime lastDate = LocalDateTime.now();
        productEntity.setLastUpdate(lastDate);
        assertEquals(lastDate, productEntity.getLastUpdate());
    }

    @Test
    void getActive() {
        ProductEntity productEntity = new ProductEntity();
        productEntity.setActive(true);
        assertTrue(productEntity.getActive());
    }

    @Test
    void getCategoryFk() {
        ProductEntity productEntity = new ProductEntity();
        CategoryEntity categoryEntity = new CategoryEntity();
        productEntity.setCategoryFk(categoryEntity);
        assertEquals(categoryEntity, productEntity.getCategoryFk());
    }

}
