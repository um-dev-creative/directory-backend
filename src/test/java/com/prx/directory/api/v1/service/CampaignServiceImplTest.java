package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.CampaignTO;
import com.prx.directory.jpa.entity.CampaignEntity;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.jpa.repository.CampaignRepository;
import com.prx.directory.jpa.repository.CategoryRepository;
import com.prx.directory.mapper.CampaignMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CampaignServiceImplTest {

    CampaignRepository campaignRepository;
    CategoryRepository categoryRepository;
    BusinessRepository businessRepository;
    CampaignMapper campaignMapper;
    CampaignServiceImpl service;

    @BeforeEach
    void setUp() {
        campaignRepository = mock(CampaignRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        businessRepository = mock(BusinessRepository.class);
        campaignMapper = mock(CampaignMapper.class);
        service = new CampaignServiceImpl(campaignRepository, categoryRepository, businessRepository, campaignMapper);
    }

    @Test
    void create_success() {
        UUID catId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        CampaignTO to = new CampaignTO(null, "N", "D", Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-12-31T00:00:00Z"), catId, busId, null, null, null);

        when(categoryRepository.existsById(catId)).thenReturn(true);
        when(businessRepository.existsById(busId)).thenReturn(true);

        CampaignEntity mapped = new CampaignEntity();
        mapped.setName(to.name());
        when(campaignMapper.toEntity(to)).thenReturn(mapped);

        CampaignEntity saved = new CampaignEntity();
        saved.setId(UUID.randomUUID());
        saved.setName(to.name());
        saved.setStartDate(to.startDate());
        saved.setEndDate(to.endDate());
        when(campaignRepository.save(any(CampaignEntity.class))).thenReturn(saved);

        when(campaignMapper.toTO(saved)).thenReturn(new CampaignTO(saved.getId(), saved.getName(), null, saved.getStartDate(), saved.getEndDate(), catId, busId, Instant.now(), Instant.now(), true));

        var resp = service.create(to);
        assertEquals(201, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertEquals(saved.getId(), resp.getBody().id());

        ArgumentCaptor<CampaignEntity> captor = ArgumentCaptor.forClass(CampaignEntity.class);
        verify(campaignRepository).save(captor.capture());
        CampaignEntity toSave = captor.getValue();
        assertNotNull(toSave.getCreatedDate());
        assertNotNull(toSave.getLastUpdate());
        assertTrue(toSave.getActive());
    }

    @Test
    void create_missingCategory() {
        UUID catId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        CampaignTO to = new CampaignTO(null, "N", "D", Instant.now(), Instant.now(), catId, busId, null, null, null);
        when(categoryRepository.existsById(catId)).thenReturn(false);
        when(businessRepository.existsById(busId)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> service.create(to));
    }

    @Test
    void create_missingBusiness() {
        UUID catId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        CampaignTO to = new CampaignTO(null, "N", "D", Instant.now(), Instant.now(), catId, busId, null, null, null);
        when(categoryRepository.existsById(catId)).thenReturn(true);
        when(businessRepository.existsById(busId)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> service.create(to));
    }

    @Test
    void create_nullIds() {
        CampaignTO to = new CampaignTO(null, "N", "D", Instant.now(), Instant.now(), null, null, null, null, null);
        assertThrows(ResponseStatusException.class, () -> service.create(to));
    }
}
