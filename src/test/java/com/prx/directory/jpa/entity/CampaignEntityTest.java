package com.prx.directory.jpa.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignEntityTest {

    @Test
    @DisplayName("Get ID Test")
    void getIdTest() {
        CampaignEntity campaignEntity = new CampaignEntity();
        UUID id = UUID.randomUUID();
        campaignEntity.setId(id);
        assertEquals(id, campaignEntity.getId());
    }

    @Test
    @DisplayName("Get Name Test")
    void getNameTest() {
        CampaignEntity campaignEntity = new CampaignEntity();
        String name = "Test Campaign";
        campaignEntity.setName(name);
        assertEquals(name, campaignEntity.getName());
    }

    @Test
    @DisplayName("Get Description Test")
    void getDescriptionTest() {
        CampaignEntity campaignEntity = new CampaignEntity();
        String description = "Test Description";
        campaignEntity.setDescription(description);
        assertEquals(description, campaignEntity.getDescription());
    }

    @Test
    @DisplayName("Get Start Date Test")
    void getStartDateTest() {
        CampaignEntity campaignEntity = new CampaignEntity();
        LocalDateTime startDate = LocalDateTime.now();
        campaignEntity.setStartDate(startDate);
        assertEquals(startDate, campaignEntity.getStartDate());
    }

    @Test
    @DisplayName("Get End Date Test")
    void getEndDateTest() {
        CampaignEntity campaignEntity = new CampaignEntity();
        LocalDateTime endDate = LocalDateTime.now();
        campaignEntity.setEndDate(endDate);
        assertEquals(endDate, campaignEntity.getEndDate());
    }

    @Test
    @DisplayName("Get Active Test")
    void getActiveTest() {
        CampaignEntity campaignEntity = new CampaignEntity();
        campaignEntity.setActive(true);
        assertTrue(campaignEntity.getActive());
    }

    @Test
    @DisplayName("Get Category Foreign Key Test")
    void getCategoryFkTest() {
        CampaignEntity campaignEntity = new CampaignEntity();
        CategoryEntity categoryEntity = new CategoryEntity();
        campaignEntity.setCategoryFk(categoryEntity);
        assertEquals(categoryEntity, campaignEntity.getCategoryFk());
    }

}
