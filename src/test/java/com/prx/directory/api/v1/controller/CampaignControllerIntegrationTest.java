package com.prx.directory.api.v1.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prx.directory.api.v1.to.CampaignUpdateRequest;
import com.prx.directory.jpa.entity.BusinessEntity;
import com.prx.directory.jpa.entity.CampaignEntity;
import com.prx.directory.jpa.entity.CategoryEntity;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.jpa.repository.CampaignRepository;
import com.prx.directory.jpa.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Campaign update endpoint.
 * These tests use an embedded H2 database and test the full request/response cycle.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CampaignControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BusinessRepository businessRepository;

    private CategoryEntity testCategory;
    private BusinessEntity testBusiness;
    private CampaignEntity testCampaign;

    @BeforeEach
    void setUp() {
        // Clean up
        campaignRepository.deleteAll();
        categoryRepository.deleteAll();
        businessRepository.deleteAll();

        // Create test data
        testCategory = new CategoryEntity();
        testCategory.setName("Test Category");
        testCategory.setDescription("Test category description");
        testCategory = categoryRepository.save(testCategory);

        testBusiness = new BusinessEntity();
        testBusiness.setName("Test Business");
        testBusiness = businessRepository.save(testBusiness);

        testCampaign = new CampaignEntity();
        testCampaign.setName("Original Campaign Name");
        testCampaign.setDescription("Original description");
        testCampaign.setStartDate(Instant.parse("2024-01-01T00:00:00Z"));
        testCampaign.setEndDate(Instant.parse("2024-12-31T23:59:59Z"));
        testCampaign.setActive(true);
        testCampaign.setCategoryFk(testCategory);
        testCampaign.setBusinessFk(testBusiness);
        testCampaign.setCreatedDate(Instant.now());
        testCampaign.setLastUpdate(Instant.now());
        testCampaign = campaignRepository.save(testCampaign);
    }

    @Test
    @DisplayName("PATCH /api/campaigns/{id} - successfully updates campaign name")
    void testUpdateCampaignName() throws Exception {
        CampaignUpdateRequest request = new CampaignUpdateRequest(
                "Updated Campaign Name",
                null, null, null, null, null, null, null
        );

        mockMvc.perform(patch("/api/campaigns/{id}", testCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testCampaign.getId().toString())))
                .andExpect(jsonPath("$.name", is("Updated Campaign Name")))
                .andExpect(jsonPath("$.description", is("Original description")))
                .andExpect(jsonPath("$.lastUpdate", notNullValue()));
    }

    @Test
    @DisplayName("PATCH /api/campaigns/{id} - successfully updates multiple fields")
    void testUpdateMultipleFields() throws Exception {
        CampaignUpdateRequest request = new CampaignUpdateRequest(
                "New Name",
                "New Description",
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-12-31T23:59:59Z"),
                null, null, false, null
        );

        mockMvc.perform(patch("/api/campaigns/{id}", testCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Name")))
                .andExpect(jsonPath("$.description", is("New Description")))
                .andExpect(jsonPath("$.active", is(false)))
                .andExpect(jsonPath("$.lastUpdate", notNullValue()));
    }

    @Test
    @DisplayName("PATCH /api/campaigns/{id} - returns 404 for non-existent campaign")
    void testUpdateNonExistentCampaign() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        CampaignUpdateRequest request = new CampaignUpdateRequest(
                "Updated Name",
                null, null, null, null, null, null, null
        );

        mockMvc.perform(patch("/api/campaigns/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/campaigns/{id} - returns 400 for name exceeding max length")
    void testUpdateWithNameTooLong() throws Exception {
        String longName = "a".repeat(121); // Max is 120
        CampaignUpdateRequest request = new CampaignUpdateRequest(
                longName,
                null, null, null, null, null, null, null
        );

        mockMvc.perform(patch("/api/campaigns/{id}", testCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/campaigns/{id} - returns 400 for description exceeding max length")
    void testUpdateWithDescriptionTooLong() throws Exception {
        String longDescription = "a".repeat(1201); // Max is 1200
        CampaignUpdateRequest request = new CampaignUpdateRequest(
                null,
                longDescription,
                null, null, null, null, null, null
        );

        mockMvc.perform(patch("/api/campaigns/{id}", testCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/campaigns/{id} - returns 400 when startDate is after endDate")
    void testUpdateWithInvalidDateRange() throws Exception {
        CampaignUpdateRequest request = new CampaignUpdateRequest(
                null, null,
                Instant.parse("2025-12-31T23:59:59Z"),
                Instant.parse("2025-01-01T00:00:00Z"),
                null, null, null, null
        );

        mockMvc.perform(patch("/api/campaigns/{id}", testCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/campaigns/{id} - returns 404 when category does not exist")
    void testUpdateWithNonExistentCategory() throws Exception {
        UUID nonExistentCategoryId = UUID.randomUUID();
        CampaignUpdateRequest request = new CampaignUpdateRequest(
                null, null, null, null,
                nonExistentCategoryId,
                null, null, null
        );

        mockMvc.perform(patch("/api/campaigns/{id}", testCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/campaigns/{id} - returns 404 when business does not exist")
    void testUpdateWithNonExistentBusiness() throws Exception {
        UUID nonExistentBusinessId = UUID.randomUUID();
        CampaignUpdateRequest request = new CampaignUpdateRequest(
                null, null, null, null, null,
                nonExistentBusinessId,
                null, null
        );

        mockMvc.perform(patch("/api/campaigns/{id}", testCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/campaigns/{id} - returns 409 on optimistic locking conflict")
    void testUpdateWithOptimisticLockingConflict() throws Exception {
        Instant oldTimestamp = Instant.parse("2020-01-01T00:00:00Z");
        CampaignUpdateRequest request = new CampaignUpdateRequest(
                "Updated Name",
                null, null, null, null, null, null,
                oldTimestamp // This doesn't match the current lastUpdate
        );

        mockMvc.perform(patch("/api/campaigns/{id}", testCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /api/campaigns/{id} - successfully updates with correct lastUpdate for optimistic locking")
    void testUpdateWithCorrectLastUpdate() throws Exception {
        Instant currentLastUpdate = testCampaign.getLastUpdate();
        CampaignUpdateRequest request = new CampaignUpdateRequest(
                "Updated Name",
                null, null, null, null, null, null,
                currentLastUpdate
        );

        mockMvc.perform(patch("/api/campaigns/{id}", testCampaign.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Name")));
    }
}
