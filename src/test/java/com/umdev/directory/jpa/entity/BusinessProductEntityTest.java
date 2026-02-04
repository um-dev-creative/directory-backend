package com.umdev.directory.jpa.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BusinessProductEntityTest {

    @Test
    @DisplayName("Create BusinessProductEntity successfully")
    void createBusinessProductEntitySuccessfully() {
        BusinessProductEntityId id = new BusinessProductEntityId(UUID.randomUUID(), UUID.randomUUID());
        BusinessEntity business = new BusinessEntity();
        ProductEntity product = new ProductEntity();
        LocalDateTime now = LocalDateTime.now();
        Boolean active = true;

        BusinessProductEntity entity = new BusinessProductEntity();
        entity.setId(id);
        entity.setBusiness(business);
        entity.setProduct(product);
        entity.setCreatedDate(now);
        entity.setLastUpdate(now);
        entity.setActive(active);

        assertEquals(id, entity.getId());
        assertEquals(business, entity.getBusiness());
        assertEquals(product, entity.getProduct());
        assertEquals(now, entity.getCreatedDate());
        assertEquals(now, entity.getLastUpdate());
        assertEquals(active, entity.getActive());
    }

    @Test
    @DisplayName("Set and get businessId successfully")
    void setAndGetBusinessIdSuccessfully() {
        BusinessProductEntity entity = new BusinessProductEntity();
        BusinessProductEntityId id = new BusinessProductEntityId(UUID.randomUUID(), UUID.randomUUID());
        entity.setId(id);

        assertEquals(id, entity.getId());
    }

    @Test
    @DisplayName("Set and get business successfully")
    void setAndGetBusinessSuccessfully() {
        BusinessProductEntity entity = new BusinessProductEntity();
        BusinessEntity business = new BusinessEntity();
        entity.setBusiness(business);

        assertEquals(business, entity.getBusiness());
    }

    @Test
    @DisplayName("Set and get product successfully")
    void setAndGetProductSuccessfully() {
        BusinessProductEntity entity = new BusinessProductEntity();
        ProductEntity product = new ProductEntity();
        entity.setProduct(product);

        assertEquals(product, entity.getProduct());
    }

    @Test
    @DisplayName("Set and get createdDate successfully")
    void setAndGetCreatedDateSuccessfully() {
        BusinessProductEntity entity = new BusinessProductEntity();
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedDate(now);

        assertEquals(now, entity.getCreatedDate());
    }

    @Test
    @DisplayName("Set and get lastUpdate successfully")
    void setAndGetLastUpdateSuccessfully() {
        BusinessProductEntity entity = new BusinessProductEntity();
        LocalDateTime now = LocalDateTime.now();
        entity.setLastUpdate(now);

        assertEquals(now, entity.getLastUpdate());
    }

    @Test
    @DisplayName("Set and get active status successfully")
    void setAndGetActiveStatusSuccessfully() {
        BusinessProductEntity entity = new BusinessProductEntity();
        Boolean active = true;
        entity.setActive(active);

        assertEquals(active, entity.getActive());
    }
}
