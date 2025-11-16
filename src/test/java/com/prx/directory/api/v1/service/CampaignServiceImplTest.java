package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.CampaignTO;
import com.prx.directory.api.v1.to.CampaignUpdateRequest;
import com.prx.directory.jpa.entity.CampaignEntity;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.jpa.repository.CampaignRepository;
import com.prx.directory.jpa.repository.CategoryRepository;
import com.prx.directory.mapper.CampaignMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CampaignServiceImplTest {

    private CampaignRepository campaignRepository;
    private CategoryRepository categoryRepository;
    private BusinessRepository businessRepository;
    private CampaignMapper campaignMapper;
    private CampaignServiceImpl service;

    @BeforeEach
    void setup() {
        campaignRepository = Mockito.mock(CampaignRepository.class);
        categoryRepository = Mockito.mock(CategoryRepository.class);
        businessRepository = Mockito.mock(BusinessRepository.class);
        campaignMapper = Mockito.mock(CampaignMapper.class);
        service = new CampaignServiceImpl(campaignRepository, categoryRepository, businessRepository, campaignMapper);
    }

    @Test
    void create_missingCategory_throws() {
        CampaignTO to = Mockito.mock(CampaignTO.class);
        when(to.categoryId()).thenReturn(null);
        when(to.businessId()).thenReturn(UUID.randomUUID());

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.create(to));
    }

    @Test
    void create_savesAndReturnsCreated() {
        UUID cat = UUID.randomUUID();
        UUID biz = UUID.randomUUID();
        CampaignTO to = new CampaignTO(null, "n", "d", null, null, cat, biz, null, null, true);

        when(categoryRepository.existsById(cat)).thenReturn(true);
        when(businessRepository.existsById(biz)).thenReturn(true);

        CampaignEntity entity = new CampaignEntity();
        when(campaignMapper.toEntity(to)).thenReturn(entity);
        CampaignEntity saved = new CampaignEntity();
        when(campaignRepository.save(entity)).thenReturn(saved);
        when(campaignMapper.toTO(saved)).thenReturn(new CampaignTO(UUID.randomUUID(), "n", "d", null, null, cat, biz, null, null, true));

        ResponseEntity<CampaignTO> result = service.create(to);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        verify(campaignRepository).save(entity);
    }

    @Test
    void update_conflict_throws() {
        UUID id = UUID.randomUUID();
        CampaignUpdateRequest req = Mockito.mock(CampaignUpdateRequest.class);
        when(req.lastUpdate()).thenReturn(Instant.parse("2025-11-01T00:00:00Z"));

        CampaignEntity existing = new CampaignEntity();
        existing.setLastUpdate(Instant.parse("2025-11-02T00:00:00Z"));
        when(campaignRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.update(id, req));
    }
}
