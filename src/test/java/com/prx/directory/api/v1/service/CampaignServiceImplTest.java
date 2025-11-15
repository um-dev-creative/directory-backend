package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.CampaignListResponse;
import com.prx.directory.api.v1.to.CampaignTO;
import com.prx.directory.api.v1.to.CampaignUpdateRequest;
import com.prx.directory.api.v1.to.OfferTO;
import com.prx.directory.jpa.entity.BusinessEntity;
import com.prx.directory.jpa.entity.CampaignEntity;
import com.prx.directory.jpa.entity.CategoryEntity;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.jpa.repository.CampaignRepository;
import com.prx.directory.jpa.repository.CategoryRepository;
import com.prx.directory.mapper.CampaignMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceImplTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private CampaignMapper campaignMapper;

    @InjectMocks
    private CampaignServiceImpl campaignService;

    private CampaignEntity entity1;
    private CampaignEntity entity2;
    private CategoryEntity categoryEntity;
    private BusinessEntity businessEntity;

    @BeforeEach
    void setUp() {
        categoryEntity = new CategoryEntity();
        categoryEntity.setId(UUID.randomUUID());

        businessEntity = new BusinessEntity();
        businessEntity.setId(UUID.randomUUID());

        entity1 = new CampaignEntity();
        entity1.setId(UUID.randomUUID());
        entity1.setName("Alpha");
        entity1.setDescription("First");
        entity1.setStartDate(Instant.parse("2024-01-01T00:00:00Z"));
        entity1.setEndDate(Instant.parse("2024-12-31T23:59:59Z"));
        entity1.setActive(true);
        entity1.setCreatedDate(Instant.parse("2024-01-01T00:00:00Z"));
        entity1.setLastUpdate(Instant.parse("2024-01-02T00:00:00Z"));
        entity1.setCategoryFk(categoryEntity);
        entity1.setBusinessFk(businessEntity);

        entity2 = new CampaignEntity();
        entity2.setId(UUID.randomUUID());
        entity2.setName("Beta");
        entity2.setDescription("Second");
        entity2.setStartDate(Instant.parse("2024-02-01T00:00:00Z"));
        entity2.setEndDate(Instant.parse("2024-11-30T23:59:59Z"));
        entity2.setActive(false);
        entity2.setCreatedDate(Instant.parse("2024-02-01T00:00:00Z"));
        entity2.setLastUpdate(Instant.parse("2024-02-02T00:00:00Z"));
    }

    @Test
    @DisplayName("list: returns paginated items with metadata and default sort")
    void listReturnsPaginatedItemsWithMetadata() {
        List<CampaignEntity> content = List.of(entity1, entity2);
        Page<CampaignEntity> page = new PageImpl<>(content, PageRequest.of(0, 2), 10);
        doReturn(page).when(campaignRepository).findAll(any(Specification.class), any(Pageable.class));
        when(campaignMapper.toOfferTO(entity1)).thenReturn(new OfferTO(entity1.getId(), entity1.getName(), entity1.getDescription(), null, entity1.getStartDate(), entity1.getEndDate(), true));
        when(campaignMapper.toOfferTO(entity2)).thenReturn(new OfferTO(entity2.getId(), entity2.getName(), entity2.getDescription(), null, entity2.getStartDate(), entity2.getEndDate(), false));

        ResponseEntity<CampaignListResponse> response = campaignService.list(null, 2, null, Collections.emptyMap());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        CampaignListResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(10, body.total_count());
        assertEquals(1, body.page());
        assertEquals(2, body.per_page());
        assertEquals(5, body.total_pages());
        assertEquals(2, body.items().size());
        verify(campaignRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("list: supports multi-field sort and returns 400 on invalid field")
    void listSortsAndValidatesSort() {
        doReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0))
                .when(campaignRepository).findAll(any(Specification.class), any(Pageable.class));

        ResponseEntity<CampaignListResponse> ok = campaignService.list(1, 20, "name,-start_date", Collections.emptyMap());
        assertEquals(HttpStatus.OK, ok.getStatusCode());

        ResponseEntity<CampaignListResponse> bad = campaignService.list(1, 20, "unknown", Collections.emptyMap());
        assertEquals(HttpStatus.BAD_REQUEST, bad.getStatusCode());
    }

    @Test
    @DisplayName("list: invalid per_page > 100 -> 400; per_page <=0 defaults to 20")
    void listValidatesPerPage() {
        ResponseEntity<CampaignListResponse> bad = campaignService.list(1, 101, null, Collections.emptyMap());
        assertEquals(HttpStatus.BAD_REQUEST, bad.getStatusCode());

        doReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0))
                .when(campaignRepository).findAll(any(Specification.class), any(Pageable.class));

        ResponseEntity<CampaignListResponse> ok = campaignService.list(1, 0, null, Collections.emptyMap());
        assertEquals(HttpStatus.OK, ok.getStatusCode());
    }

    @Test
    @DisplayName("list: accepts filters map (pass-through)")
    void listAcceptsFilters() {
        Map<String, String> filters = new HashMap<>();
        filters.put("q", "alp");
        filters.put("active", "true");
        filters.put("business_id", UUID.randomUUID().toString());
        filters.put("category_id", UUID.randomUUID().toString());
        filters.put("start_date_from", "2024-01-01T00:00:00Z");
        filters.put("end_date_to", "2024-12-31T23:59:59Z");

        doReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0))
                .when(campaignRepository).findAll(any(Specification.class), any(Pageable.class));

        ResponseEntity<CampaignListResponse> ok = campaignService.list(null, null, null, filters);
        assertEquals(HttpStatus.OK, ok.getStatusCode());
        verify(campaignRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("list: returns 400 for malformed sort strings with empty tokens")
    void listRejectsEmptyTokensInSort() {
        ResponseEntity<CampaignListResponse> bad1 = campaignService.list(1, 20, "name,,end_date", Collections.emptyMap());
        assertEquals(HttpStatus.BAD_REQUEST, bad1.getStatusCode());

        ResponseEntity<CampaignListResponse> bad2 = campaignService.list(1, 20, ",name", Collections.emptyMap());
        assertEquals(HttpStatus.BAD_REQUEST, bad2.getStatusCode());
    }

    @Test
    @DisplayName("update: successfully updates campaign name")
    void updateSuccessfullyUpdatesName() {
        UUID campaignId = entity1.getId();
        CampaignUpdateRequest request = new CampaignUpdateRequest("Updated Name", null, null, null, null, null, null, null);
        
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(entity1));
        when(campaignRepository.save(any(CampaignEntity.class))).thenReturn(entity1);
        when(campaignMapper.toTO(entity1)).thenReturn(new CampaignTO(campaignId, "Updated Name", "First", 
                entity1.getStartDate(), entity1.getEndDate(), categoryEntity.getId(), businessEntity.getId(), 
                entity1.getCreatedDate(), entity1.getLastUpdate(), true));

        ResponseEntity<CampaignTO> response = campaignService.update(campaignId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(campaignRepository).save(any(CampaignEntity.class));
    }

    @Test
    @DisplayName("update: returns 404 when campaign not found")
    void updateReturns404WhenCampaignNotFound() {
        UUID campaignId = UUID.randomUUID();
        CampaignUpdateRequest request = new CampaignUpdateRequest("Updated Name", null, null, null, null, null, null, null);
        
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> campaignService.update(campaignId, request));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName("update: returns 409 on optimistic locking conflict")
    void updateReturns409OnOptimisticLockingConflict() {
        UUID campaignId = entity1.getId();
        Instant oldTimestamp = Instant.parse("2024-01-01T00:00:00Z");
        CampaignUpdateRequest request = new CampaignUpdateRequest("Updated Name", null, null, null, null, null, null, oldTimestamp);
        
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(entity1));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> campaignService.update(campaignId, request));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    @DisplayName("update: validates startDate <= endDate")
    void updateValidatesDateConstraint() {
        UUID campaignId = entity1.getId();
        Instant futureStart = Instant.parse("2025-01-01T00:00:00Z");
        Instant pastEnd = Instant.parse("2024-01-01T00:00:00Z");
        CampaignUpdateRequest request = new CampaignUpdateRequest(null, null, futureStart, pastEnd, null, null, null, null);
        
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(entity1));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> campaignService.update(campaignId, request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("startDate must be before or equal to endDate"));
    }

    @Test
    @DisplayName("update: returns 404 when category not found")
    void updateReturns404WhenCategoryNotFound() {
        UUID campaignId = entity1.getId();
        UUID newCategoryId = UUID.randomUUID();
        CampaignUpdateRequest request = new CampaignUpdateRequest(null, null, null, null, newCategoryId, null, null, null);
        
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(entity1));
        when(categoryRepository.existsById(newCategoryId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> campaignService.update(campaignId, request));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Category not found"));
    }

    @Test
    @DisplayName("update: returns 404 when business not found")
    void updateReturns404WhenBusinessNotFound() {
        UUID campaignId = entity1.getId();
        UUID newBusinessId = UUID.randomUUID();
        CampaignUpdateRequest request = new CampaignUpdateRequest(null, null, null, null, null, newBusinessId, null, null);
        
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(entity1));
        when(businessRepository.existsById(newBusinessId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> campaignService.update(campaignId, request));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Business not found"));
    }

    @Test
    @DisplayName("update: successfully updates multiple fields")
    void updateSuccessfullyUpdatesMultipleFields() {
        UUID campaignId = entity1.getId();
        UUID newCategoryId = UUID.randomUUID();
        CampaignUpdateRequest request = new CampaignUpdateRequest("New Name", "New Description", 
                null, null, newCategoryId, null, false, null);
        
        CategoryEntity newCategory = new CategoryEntity();
        newCategory.setId(newCategoryId);
        
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(entity1));
        when(categoryRepository.existsById(newCategoryId)).thenReturn(true);
        when(campaignRepository.save(any(CampaignEntity.class))).thenReturn(entity1);
        when(campaignMapper.toTO(entity1)).thenReturn(new CampaignTO(campaignId, "New Name", "New Description", 
                entity1.getStartDate(), entity1.getEndDate(), newCategoryId, businessEntity.getId(), 
                entity1.getCreatedDate(), Instant.now(), false));

        ResponseEntity<CampaignTO> response = campaignService.update(campaignId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(campaignRepository).save(any(CampaignEntity.class));
    }

    @Test
    @DisplayName("update: returns 400 when id is null")
    void updateReturns400WhenIdIsNull() {
        CampaignUpdateRequest request = new CampaignUpdateRequest("Updated Name", null, null, null, null, null, null, null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> campaignService.update(null, request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("update: returns 400 when request is null")
    void updateReturns400WhenRequestIsNull() {
        UUID campaignId = UUID.randomUUID();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
                () -> campaignService.update(campaignId, null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
