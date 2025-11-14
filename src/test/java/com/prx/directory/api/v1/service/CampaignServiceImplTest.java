package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.CampaignListResponse;
import com.prx.directory.api.v1.to.OfferTO;
import com.prx.directory.jpa.entity.CampaignEntity;
import com.prx.directory.jpa.repository.CampaignRepository;
import com.prx.directory.mapper.CampaignMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
    private CampaignMapper campaignMapper;

    @InjectMocks
    private CampaignServiceImpl campaignService;

    private CampaignEntity entity1;
    private CampaignEntity entity2;

    @BeforeEach
    void setUp() {
        entity1 = new CampaignEntity();
        entity1.setId(UUID.randomUUID());
        entity1.setName("Alpha");
        entity1.setDescription("First");
        entity1.setStartDate(Instant.parse("2024-01-01T00:00:00Z"));
        entity1.setEndDate(Instant.parse("2024-12-31T23:59:59Z"));
        entity1.setActive(true);
        entity1.setCreatedDate(Instant.parse("2024-01-01T00:00:00Z"));
        entity1.setLastUpdate(Instant.parse("2024-01-02T00:00:00Z"));

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
        when(campaignRepository.findAll(any(Pageable.class))).thenReturn(page);
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
        verify(campaignRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("list: supports multi-field sort and returns 400 on invalid field")
    void listSortsAndValidatesSort() {
        when(campaignRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0));

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

        when(campaignRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0));

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

        when(campaignRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0));

        ResponseEntity<CampaignListResponse> ok = campaignService.list(null, null, null, filters);
        assertEquals(HttpStatus.OK, ok.getStatusCode());
        verify(campaignRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("list: returns 400 for malformed sort strings with empty tokens")
    void listRejectsEmptyTokensInSort() {
        ResponseEntity<CampaignListResponse> bad1 = campaignService.list(1, 20, "name,,end_date", Collections.emptyMap());
        assertEquals(HttpStatus.BAD_REQUEST, bad1.getStatusCode());

        ResponseEntity<CampaignListResponse> bad2 = campaignService.list(1, 20, "name,", Collections.emptyMap());
        assertEquals(HttpStatus.BAD_REQUEST, bad2.getStatusCode());

        ResponseEntity<CampaignListResponse> bad3 = campaignService.list(1, 20, ",name", Collections.emptyMap());
        assertEquals(HttpStatus.BAD_REQUEST, bad3.getStatusCode());
    }
}
