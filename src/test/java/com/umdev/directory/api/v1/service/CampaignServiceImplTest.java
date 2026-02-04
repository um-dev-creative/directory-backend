package com.umdev.directory.api.v1.service;

import com.umdev.directory.api.v1.to.CampaignTO;
import com.umdev.directory.api.v1.to.CampaignUpdateRequest;
import com.umdev.directory.jpa.entity.CampaignEntity;
import com.umdev.directory.jpa.repository.BusinessRepository;
import com.umdev.directory.jpa.repository.CampaignRepository;
import com.umdev.directory.jpa.repository.CategoryRepository;
import com.umdev.directory.mapper.CampaignMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        CampaignTO to = new CampaignTO(null, "n", "d", null, null, cat, biz, null, null, null, true);

        when(categoryRepository.existsById(cat)).thenReturn(true);
        when(businessRepository.existsById(biz)).thenReturn(true);

        CampaignEntity entity = new CampaignEntity();
        when(campaignMapper.toEntity(to)).thenReturn(entity);
        CampaignEntity saved = new CampaignEntity();
        when(campaignRepository.save(entity)).thenReturn(saved);
        when(campaignMapper.toTO(saved)).thenReturn(new CampaignTO(UUID.randomUUID(), "n", "d", null, null, cat, biz, null, null, null, true));

        ResponseEntity<CampaignTO> result = service.create(to);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        verify(campaignRepository).save(entity);
    }

    @Test
    void update_conflict_throws() {
        UUID id = UUID.randomUUID();
        CampaignUpdateRequest req = Mockito.mock(CampaignUpdateRequest.class);
        when(req.lastUpdate()).thenReturn(LocalDateTime.parse("2025-11-01T00:00:00"));

        CampaignEntity existing = new CampaignEntity();
        existing.setLastUpdate(LocalDateTime.parse("2025-11-02T00:00:00"));
        when(campaignRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.update(id, req));
    }

    @Test
    void list_returnsCountsAndItems() {
        // Prepare pageable result
        CampaignEntity e1 = new CampaignEntity();
        e1.setId(UUID.randomUUID());
        CampaignEntity e2 = new CampaignEntity();
        e2.setId(UUID.randomUUID());
        var page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(e1, e2));

        // Mock repository findAll to return the page
        when(campaignRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.jpa.domain.Specification.class), org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        // Mock counts via generic count(Specification)
        when(campaignRepository.count(org.mockito.ArgumentMatchers.any(Specification.class))).thenAnswer(inv -> {
            Specification spec = inv.getArgument(0);
            // crude heuristic: called three times; return 6,5,5 in order
            // use an internal counter stored in a lambda captured array
            return (long) TestCounter.next();
        });

        // Reset TestCounter so first three calls map to 6,5,5
        TestCounter.reset(new long[]{6L,5L,5L});

        // Mock mapper mapping
        when(campaignMapper.toTO(e1)).thenReturn(new CampaignTO(e1.getId(), "t1", null, null, null, null, null, null, null, null, true));
        when(campaignMapper.toTO(e2)).thenReturn(new CampaignTO(e2.getId(), "t2", null, null, null, null, null, null, null, null, true));

        var resp = service.list(1, 10, "-start_date", java.util.Collections.emptyMap());
        assertEquals(org.springframework.http.HttpStatus.OK, resp.getStatusCode());
        var body = resp.getBody();
        assertEquals(6L, body.actives());
        assertEquals(5L, body.inactives());
        assertEquals(5L, body.expired());
        assertEquals(2, body.items().size());
    }
}

// Simple test helper to simulate ordered returns from repository.count when invoked multiple times
class TestCounter {
    private static long[] values = new long[]{6L,5L,5L};
    private static int idx = 0;
    static void reset(long[] v) { values = v; idx = 0; }
    static long next() { if (idx >= values.length) return values[values.length-1]; return values[idx++]; }
}
