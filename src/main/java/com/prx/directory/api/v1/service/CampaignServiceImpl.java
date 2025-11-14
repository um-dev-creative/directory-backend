package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.CampaignListResponse;
import com.prx.directory.api.v1.to.CampaignTO;
import com.prx.directory.api.v1.to.OfferTO;
import com.prx.directory.jpa.entity.CampaignEntity;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.jpa.repository.CampaignRepository;
import com.prx.directory.jpa.repository.CategoryRepository;
import com.prx.directory.mapper.CampaignMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CampaignServiceImpl implements CampaignService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PER_PAGE = 20;
    private static final int MAX_PER_PAGE = 100;

    private static final Map<String, String> SORT_FIELD_MAPPING = Map.of(
            "name", "name",
            "start_date", "startDate",
            "end_date", "endDate",
            "created_date", "createdDate"
    );

    private final CampaignRepository campaignRepository;
    private final CategoryRepository categoryRepository;
    private final BusinessRepository businessRepository;
    private final CampaignMapper campaignMapper;

    public CampaignServiceImpl(CampaignRepository campaignRepository,
                               CategoryRepository categoryRepository,
                               BusinessRepository businessRepository,
                               CampaignMapper campaignMapper) {
        this.campaignRepository = campaignRepository;
        this.categoryRepository = categoryRepository;
        this.businessRepository = businessRepository;
        this.campaignMapper = campaignMapper;
    }

    @Override
    @Transactional
    public ResponseEntity<CampaignTO> create(CampaignTO campaignTO) {
        // Validate FK existence
        UUID categoryId = campaignTO.categoryId();
        UUID businessId = campaignTO.businessId();

        if (categoryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryId is required");
        }
        if (businessId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "businessId is required");
        }

        if (!categoryRepository.existsById(categoryId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category not found");
        }
        if (!businessRepository.existsById(businessId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "business not found");
        }

        CampaignEntity entity = campaignMapper.toEntity(campaignTO);
        Instant now = Instant.now();
        entity.setCreatedDate(now);
        entity.setLastUpdate(now);
        if (entity.getActive() == null) {
            entity.setActive(true);
        }

        CampaignEntity saved = campaignRepository.save(entity);
        CampaignTO result = campaignMapper.toTO(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Override
    public ResponseEntity<CampaignListResponse> list(Integer page, Integer perPage, String sort, Map<String, String> filters) {
        int p = (page == null || page < 1) ? DEFAULT_PAGE : page;
        int pp = (perPage == null || perPage < 1) ? DEFAULT_PER_PAGE : perPage;
        if (pp > MAX_PER_PAGE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Sort sortObj = parseSort(sort);
        if (sortObj == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Pageable pageable = PageRequest.of(p - 1, pp, sortObj);
        Page<CampaignEntity> result = campaignRepository.findAll(pageable);

        List<OfferTO> items = result.getContent().stream().map(campaignMapper::toOfferTO).collect(Collectors.toList());

        CampaignListResponse response = new CampaignListResponse(
                result.getTotalElements(),
                p,
                pp,
                result.getTotalPages(),
                items
        );
        return ResponseEntity.ok(response);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Order.desc(SORT_FIELD_MAPPING.get("created_date")));
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (String token : sort.split(",")) {
            String t = token.trim();
            if (t.isEmpty()) return null;
            boolean desc = t.startsWith("-");
            String key = desc ? t.substring(1) : t;
            String property = SORT_FIELD_MAPPING.get(key);
            if (property == null) return null;
            orders.add(desc ? Sort.Order.desc(property) : Sort.Order.asc(property));
        }
        return orders.isEmpty() ? null : Sort.by(orders);
    }
}
