package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.CampaignListResponse;
import com.prx.directory.api.v1.to.CampaignTO;
import com.prx.directory.api.v1.to.CampaignUpdateRequest;
import com.prx.directory.api.v1.to.OfferTO;
import com.prx.directory.jpa.entity.CampaignEntity;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.jpa.repository.CampaignRepository;
import com.prx.directory.jpa.repository.CategoryRepository;
import com.prx.directory.mapper.CampaignMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampaignServiceImplAllTest {

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

    // -------- create(...) tests --------

    @Test
    void create_missingCategory_throwsBadRequest() {
        CampaignTO to = new CampaignTO(null, "n", "d", null, null, null, UUID.randomUUID(), null, null, true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(to));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void create_categoryNotFound_throwsBadRequest() {
        UUID cat = UUID.randomUUID();
        UUID biz = UUID.randomUUID();
        CampaignTO to = new CampaignTO(null, "n", "d", null, null, cat, biz, null, null, null);
        when(categoryRepository.existsById(cat)).thenReturn(false);
        when(businessRepository.existsById(biz)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(to));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void create_missingBusiness_throwsBadRequest() {
        CampaignTO to = new CampaignTO(null, "n", "d", null, null, UUID.randomUUID(), null, null, null, true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(to));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void create_businessNotFound_throwsBadRequest() {
        UUID cat = UUID.randomUUID();
        UUID biz = UUID.randomUUID();
        CampaignTO to = new CampaignTO(null, "n", "d", null, null, cat, biz, null, null, null);
        when(categoryRepository.existsById(cat)).thenReturn(true);
        when(businessRepository.existsById(biz)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(to));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void create_success_setsTimestampsAndActive() {
        UUID cat = UUID.randomUUID();
        UUID biz = UUID.randomUUID();
        CampaignTO to = new CampaignTO(null, "n", "d", null, null, cat, biz, null, null, null);
        when(categoryRepository.existsById(cat)).thenReturn(true);
        when(businessRepository.existsById(biz)).thenReturn(true);

        CampaignEntity entity = new CampaignEntity();
        entity.setActive(null);
        when(campaignMapper.toEntity(to)).thenReturn(entity);
        CampaignEntity savedEntity = new CampaignEntity();
        when(campaignRepository.save(entity)).thenReturn(savedEntity);
        when(campaignMapper.toTO(savedEntity)).thenReturn(new CampaignTO(UUID.randomUUID(), "n", "d", null, null, cat, biz, null, null, true));

        ResponseEntity<CampaignTO> resp = service.create(to);
        assertEquals(201, resp.getStatusCode().value());

        ArgumentCaptor<CampaignEntity> captor = ArgumentCaptor.forClass(CampaignEntity.class);
        verify(campaignRepository).save(captor.capture());
        CampaignEntity passed = captor.getValue();
        assertNotNull(passed.getCreatedDate());
        assertNotNull(passed.getLastUpdate());
        assertTrue(passed.getActive());
    }

    @Test
    void create_activeAlreadySet_doesNotOverride() {
        UUID cat = UUID.randomUUID();
        UUID biz = UUID.randomUUID();
        CampaignTO to = new CampaignTO(null, "n", "d", null, null, cat, biz, null, null, false);
        when(categoryRepository.existsById(cat)).thenReturn(true);
        when(businessRepository.existsById(biz)).thenReturn(true);

        CampaignEntity entity = new CampaignEntity();
        entity.setActive(false); // Already set
        when(campaignMapper.toEntity(to)).thenReturn(entity);
        CampaignEntity savedEntity = new CampaignEntity();
        when(campaignRepository.save(entity)).thenReturn(savedEntity);
        when(campaignMapper.toTO(savedEntity)).thenReturn(new CampaignTO(UUID.randomUUID(), "n", "d", null, null, cat, biz, null, null, false));

        ResponseEntity<CampaignTO> resp = service.create(to);
        assertEquals(201, resp.getStatusCode().value());

        ArgumentCaptor<CampaignEntity> captor = ArgumentCaptor.forClass(CampaignEntity.class);
        verify(campaignRepository).save(captor.capture());
        CampaignEntity passed = captor.getValue();
        assertFalse(passed.getActive()); // Should remain false
    }

    // -------- find(...) tests --------

    @Test
    void find_existing_returnsOk() {
        UUID id = UUID.randomUUID();
        CampaignEntity entity = new CampaignEntity();
        when(campaignRepository.findById(id)).thenReturn(Optional.of(entity));
        when(campaignMapper.toTO(entity)).thenReturn(new CampaignTO(id, "n", "d", null, null, UUID.randomUUID(), UUID.randomUUID(), null, null, true));

        ResponseEntity<CampaignTO> resp = service.find(id);
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void find_missing_returnsNotFound() {
        UUID id = UUID.randomUUID();
        when(campaignRepository.findById(id)).thenReturn(Optional.empty());
        ResponseEntity<CampaignTO> resp = service.find(id);
        assertEquals(404, resp.getStatusCode().value());
    }

    // -------- list(...) tests --------

    @Test
    void list_perPageTooLarge_returnsBadRequest() {
        ResponseEntity<CampaignListResponse> resp = service.list(1, 1000, "name", Map.of());
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void list_invalidSort_returnsBadRequest() {
        // CampaignSortParser.parse("bad") returns null -> BAD_REQUEST
        ResponseEntity<CampaignListResponse> resp = service.list(1, 10, "badfield", Map.of());
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void list_filterParseThrows_returnsBadRequest() {
        Map<String, String> filters = new HashMap<>();
        filters.put("active", "notabool"); // will cause parseActive to throw
        ResponseEntity<CampaignListResponse> resp = service.list(1, 10, "name", filters);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void list_nullPage_usesDefault() {
        CampaignEntity e = new CampaignEntity();
        List<CampaignEntity> content = List.of(e);
        Page<CampaignEntity> page = new PageImpl<>(content);
        when(campaignRepository.findAll((org.springframework.data.jpa.domain.Specification<CampaignEntity>) any(), (Pageable) any())).thenReturn(page);
        when(campaignMapper.toOfferTO(e)).thenReturn(new OfferTO(UUID.randomUUID(), "title", "desc", UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(3600), true));

        ResponseEntity<CampaignListResponse> resp = service.list(null, 10, "name", Map.of());
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void list_zeroPage_usesDefault() {
        CampaignEntity e = new CampaignEntity();
        List<CampaignEntity> content = List.of(e);
        Page<CampaignEntity> page = new PageImpl<>(content);
        when(campaignRepository.findAll((org.springframework.data.jpa.domain.Specification<CampaignEntity>) any(), (Pageable) any())).thenReturn(page);
        when(campaignMapper.toOfferTO(e)).thenReturn(new OfferTO(UUID.randomUUID(), "title", "desc", UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(3600), true));

        ResponseEntity<CampaignListResponse> resp = service.list(0, 10, "name", Map.of());
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void list_nullPerPage_usesDefault() {
        CampaignEntity e = new CampaignEntity();
        List<CampaignEntity> content = List.of(e);
        Page<CampaignEntity> page = new PageImpl<>(content);
        when(campaignRepository.findAll((org.springframework.data.jpa.domain.Specification<CampaignEntity>) any(), (Pageable) any())).thenReturn(page);
        when(campaignMapper.toOfferTO(e)).thenReturn(new OfferTO(UUID.randomUUID(), "title", "desc", UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(3600), true));

        ResponseEntity<CampaignListResponse> resp = service.list(1, null, "name", Map.of());
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void update_nullId_throwsBadRequest() {
        CampaignUpdateRequest req = new CampaignUpdateRequest(null, null, null, null, null, null, null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.update(null, req));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void update_nullRequest_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.update(id, null));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void update_notFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(campaignRepository.findById(id)).thenReturn(Optional.empty());
        CampaignUpdateRequest req = new CampaignUpdateRequest(null, null, null, null, null, null, null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.update(id, req));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void update_conflictOnLastUpdate_throwsConflict() {
        UUID id = UUID.randomUUID();
        CampaignEntity existing = new CampaignEntity();
        existing.setLastUpdate(Instant.parse("2025-11-02T00:00:00Z"));
        when(campaignRepository.findById(id)).thenReturn(Optional.of(existing));

        CampaignUpdateRequest req = new CampaignUpdateRequest(null, null, null, null, null, null, null, Instant.parse("2025-11-01T00:00:00Z"));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.update(id, req));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void update_dateValidation_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        CampaignEntity existing = new CampaignEntity();
        existing.setStartDate(Instant.parse("2025-11-01T00:00:00Z"));
        existing.setEndDate(Instant.parse("2025-11-05T00:00:00Z"));
        when(campaignRepository.findById(id)).thenReturn(Optional.of(existing));

        // Provide request that makes newStartDate > newEndDate
        CampaignUpdateRequest req = new CampaignUpdateRequest(null, null, Instant.parse("2025-11-10T00:00:00Z"), Instant.parse("2025-11-02T00:00:00Z"), null, null, null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.update(id, req));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void update_relationCategoryNotFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        CampaignEntity existing = new CampaignEntity();
        existing.setLastUpdate(null);
        when(campaignRepository.findById(id)).thenReturn(Optional.of(existing));

        UUID missingCategory = UUID.randomUUID();
        CampaignUpdateRequest req = new CampaignUpdateRequest(null, null, null, null, missingCategory, null, null, null);

        when(categoryRepository.existsById(missingCategory)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.update(id, req));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void update_businessNotFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        CampaignEntity existing = new CampaignEntity();
        existing.setLastUpdate(null);
        when(campaignRepository.findById(id)).thenReturn(Optional.of(existing));

        UUID missingBusiness = UUID.randomUUID();
        CampaignUpdateRequest req = new CampaignUpdateRequest(null, null, null, null, null, missingBusiness, null, null);

        when(businessRepository.existsById(missingBusiness)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.update(id, req));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void update_success_updatesFieldsAndSaves() {
        UUID id = UUID.randomUUID();
        CampaignEntity existing = new CampaignEntity();
        existing.setLastUpdate(null);
        existing.setName("old");
        when(campaignRepository.findById(id)).thenReturn(Optional.of(existing));

        CampaignUpdateRequest req = new CampaignUpdateRequest("newName", "newDesc", null, null, null, null, true, null);
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(campaignMapper.toTO(any())).thenReturn(new CampaignTO(id, "newName", "newDesc", null, null, null, null, null, null, true));

        ResponseEntity<CampaignTO> resp = service.update(id, req);
        assertEquals(200, resp.getStatusCode().value());
        ArgumentCaptor<CampaignEntity> captor = ArgumentCaptor.forClass(CampaignEntity.class);
        verify(campaignRepository).save(captor.capture());
        CampaignEntity saved = captor.getValue();
        assertEquals("newName", saved.getName());
        assertEquals("newDesc", saved.getDescription());
        assertNotNull(saved.getLastUpdate());
    }

    @Test
    void update_noFieldsChanged_doesNotUpdateTimestamp() {
        UUID id = UUID.randomUUID();
        CampaignEntity existing = new CampaignEntity();
        existing.setLastUpdate(Instant.parse("2025-11-01T00:00:00Z"));
        existing.setName("oldName");
        when(campaignRepository.findById(id)).thenReturn(Optional.of(existing));

        // Empty request - no fields to update
        CampaignUpdateRequest req = new CampaignUpdateRequest(null, null, null, null, null, null, null, null);
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(campaignMapper.toTO(any())).thenReturn(new CampaignTO(id, "oldName", null, null, null, null, null, null, null, true));

        ResponseEntity<CampaignTO> resp = service.update(id, req);
        assertEquals(200, resp.getStatusCode().value());

        ArgumentCaptor<CampaignEntity> captor = ArgumentCaptor.forClass(CampaignEntity.class);
        verify(campaignRepository).save(captor.capture());
        CampaignEntity saved = captor.getValue();
        // lastUpdate should not change when no fields updated
        assertEquals(Instant.parse("2025-11-01T00:00:00Z"), saved.getLastUpdate());
    }

    @Test
    void update_onlyDatesChanged_updatesTimestamp() {
        UUID id = UUID.randomUUID();
        CampaignEntity existing = new CampaignEntity();
        existing.setLastUpdate(null);
        existing.setStartDate(null);
        existing.setEndDate(null);
        when(campaignRepository.findById(id)).thenReturn(Optional.of(existing));

        CampaignUpdateRequest req = new CampaignUpdateRequest(null, null, Instant.parse("2025-11-01T00:00:00Z"), Instant.parse("2025-11-05T00:00:00Z"), null, null, null, null);
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(campaignMapper.toTO(any())).thenReturn(new CampaignTO(id, null, null, null, null, null, null, null, null, true));

        ResponseEntity<CampaignTO> resp = service.update(id, req);
        assertEquals(200, resp.getStatusCode().value());

        ArgumentCaptor<CampaignEntity> captor = ArgumentCaptor.forClass(CampaignEntity.class);
        verify(campaignRepository).save(captor.capture());
        CampaignEntity saved = captor.getValue();
        assertNotNull(saved.getLastUpdate());
        assertEquals(Instant.parse("2025-11-01T00:00:00Z"), saved.getStartDate());
        assertEquals(Instant.parse("2025-11-05T00:00:00Z"), saved.getEndDate());
    }

    @Test
    void update_categoryIdAndBusinessIdBothUpdated_succeeds() {
        UUID id = UUID.randomUUID();
        CampaignEntity existing = new CampaignEntity();
        existing.setLastUpdate(null);
        when(campaignRepository.findById(id)).thenReturn(Optional.of(existing));

        UUID newCat = UUID.randomUUID();
        UUID newBiz = UUID.randomUUID();
        CampaignUpdateRequest req = new CampaignUpdateRequest(null, null, null, null, newCat, newBiz, null, null);

        when(categoryRepository.existsById(newCat)).thenReturn(true);
        when(businessRepository.existsById(newBiz)).thenReturn(true);
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(campaignMapper.toTO(any())).thenReturn(new CampaignTO(id, null, null, null, null, newCat, newBiz, null, null, true));

        ResponseEntity<CampaignTO> resp = service.update(id, req);
        assertEquals(200, resp.getStatusCode().value());

        ArgumentCaptor<CampaignEntity> captor = ArgumentCaptor.forClass(CampaignEntity.class);
        verify(campaignRepository).save(captor.capture());
        CampaignEntity saved = captor.getValue();
        assertNotNull(saved.getCategoryFk());
        assertNotNull(saved.getBusinessFk());
        assertNotNull(saved.getLastUpdate());
    }

    // -------- Additional edge case tests --------

    @Test
    void list_emptyPage_returnsOkWithEmptyItems() {
        Page<CampaignEntity> emptyPage = new PageImpl<>(Collections.emptyList());
        when(campaignRepository.findAll((Specification<CampaignEntity>) any(), (Pageable) any())).thenReturn(emptyPage);

        ResponseEntity<CampaignListResponse> resp = service.list(1, 10, "name", Map.of());
        assertEquals(200, resp.getStatusCode().value());
        CampaignListResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals(0, body.items().size());
        assertEquals(0, body.total_count());
    }

    @Test
    void list_repositoryThrowsException_returnsInternalServerError() {
        when(campaignRepository.findAll((Specification<CampaignEntity>) any(), (Pageable) any()))
                .thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class, () -> service.list(1, 10, "name", Map.of()));
    }

    @Test
    void list_withAllValidFilters_succeeds() {
        Map<String, String> filters = new HashMap<>();
        filters.put("name", "Holiday");
        filters.put("active", "true");
        filters.put("category_fk", UUID.randomUUID().toString());
        filters.put("business_fk", UUID.randomUUID().toString());
        filters.put("start_from", "2025-11-01T00:00:00Z");
        filters.put("start_to", "2025-11-30T23:59:59Z");

        CampaignEntity e = new CampaignEntity();
        Page<CampaignEntity> page = new PageImpl<>(List.of(e));
        when(campaignRepository.findAll((Specification<CampaignEntity>) any(), (Pageable) any())).thenReturn(page);
        when(campaignMapper.toOfferTO(e)).thenReturn(new OfferTO(UUID.randomUUID(), "title", "desc", UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(3600), true));

        ResponseEntity<CampaignListResponse> resp = service.list(1, 10, "name", filters);
        assertEquals(200, resp.getStatusCode().value());
    }
}
