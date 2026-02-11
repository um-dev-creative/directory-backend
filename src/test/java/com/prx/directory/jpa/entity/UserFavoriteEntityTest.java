package com.prx.directory.jpa.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserFavoriteEntityTest {

    @Test
    @DisplayName("Get ID")
    void getId() {
        UserFavoriteEntity entity = new UserFavoriteEntity();
        UUID id = UUID.randomUUID();
        entity.setId(id);
        assertEquals(id, entity.getId());
    }

    @Test
    @DisplayName("Set ID to Null")
    void setIdToNull() {
        UserFavoriteEntity entity = new UserFavoriteEntity();
        entity.setId(null);
        assertNull(entity.getId());
    }

    @Test
    @DisplayName("User association")
    void userAssociation() {
        UserFavoriteEntity entity = new UserFavoriteEntity();
        UserEntity user = new UserEntity();
        UUID id = UUID.randomUUID();
        user.setId(id);
        entity.setUser(user);
        assertEquals(user, entity.getUser());
        assertEquals(id, entity.getUser().getId());
    }

    @Test
    @DisplayName("Business association")
    void businessAssociation() {
        UserFavoriteEntity entity = new UserFavoriteEntity();
        BusinessEntity business = new BusinessEntity();
        entity.setBusiness(business);
        assertEquals(business, entity.getBusiness());
    }

    @Test
    @DisplayName("Product association")
    void productAssociation() {
        UserFavoriteEntity entity = new UserFavoriteEntity();
        ProductEntity product = new ProductEntity();
        entity.setProduct(product);
        assertEquals(product, entity.getProduct());
    }

    @Test
    @DisplayName("Campaign association")
    void campaignAssociation() {
        UserFavoriteEntity entity = new UserFavoriteEntity();
        CampaignEntity campaign = new CampaignEntity();
        entity.setCampaign(campaign);
        assertEquals(campaign, entity.getCampaign());
    }

    @Test
    @DisplayName("Active default and setter")
    void activeDefaultAndSetter() {
        UserFavoriteEntity entity = new UserFavoriteEntity();
        // In the entity the field is initialized to true; verify default Java value
        assertTrue(entity.getActive());
        entity.setActive(true);
        assertTrue(entity.getActive());
    }

    @Test
    @DisplayName("Created and Updated dates")
    void createdAndUpdatedDates() {
        UserFavoriteEntity entity = new UserFavoriteEntity();
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now.plusDays(1));
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now.plusDays(1), entity.getUpdatedAt());
    }

    @Test
    @DisplayName("Implements Serializable")
    void implementsSerializable() {
        UserFavoriteEntity entity = new UserFavoriteEntity();
        assertInstanceOf(Serializable.class, entity);
    }
}

