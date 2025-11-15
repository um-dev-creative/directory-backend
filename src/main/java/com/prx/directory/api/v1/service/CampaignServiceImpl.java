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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CampaignServiceImpl implements CampaignService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PER_PAGE = 20;
    private static final int MAX_PER_PAGE = 100;

    private final CampaignRepository campaignRepository;
    private final CategoryRepository categoryRepository;
    private final BusinessRepository businessRepository;
    private final CampaignMapper campaignMapper;
    private final CampaignFilterParser filterParser;

    public CampaignServiceImpl(CampaignRepository campaignRepository,
                               CategoryRepository categoryRepository,
                               BusinessRepository businessRepository,
                               CampaignMapper campaignMapper) {
        this.campaignRepository = campaignRepository;
        this.categoryRepository = categoryRepository;
        this.businessRepository = businessRepository;
        this.campaignMapper = campaignMapper;
        this.filterParser = new CampaignFilterParser();
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
    public ResponseEntity<CampaignTO> find(UUID id) {
        return campaignRepository.findById(id)
                .map(campaignMapper::toTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Override
    public ResponseEntity<CampaignListResponse> list(Integer page, Integer perPage, String sort, Map<String, String> filters) {
        int p = (page == null || page < 1) ? DEFAULT_PAGE : page;
        int pp = (perPage == null || perPage < 1) ? DEFAULT_PER_PAGE : perPage;
        if (pp > MAX_PER_PAGE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Sort sortObj = CampaignSortParser.parse(sort);
        if (sortObj == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            // Parse all filters using helper
            String name = filterParser.parseName(filters);
            UUID categoryId = filterParser.parseCategoryId(filters);
            UUID businessId = filterParser.parseBusinessId(filters);
            Boolean active = filterParser.parseActive(filters);

            Instant startFrom = filterParser.parseStartFrom(filters);
            Instant startTo = filterParser.parseStartTo(filters);
            Instant endFrom = filterParser.parseEndFrom(filters);
            Instant endTo = filterParser.parseEndTo(filters);

            // Validate date ranges
            filterParser.validateDateRanges(startFrom, startTo, endFrom, endTo);

            Pageable pageable = PageRequest.of(p - 1, pp, sortObj);
            var criteria = com.prx.directory.jpa.spec.CampaignCriteria.of(active, startFrom, startTo, endFrom, endTo);
            Specification<CampaignEntity> spec = com.prx.directory.jpa.spec.CampaignSpecifications
                    .byFilters(name, categoryId, businessId, criteria);
            Page<CampaignEntity> result = campaignRepository.findAll(spec, pageable);

            List<OfferTO> items = result.getContent().stream()
                    .map(campaignMapper::toOfferTO)
                    .toList();

            CampaignListResponse response = new CampaignListResponse(
                    result.getTotalElements(),
                    p,
                    pp,
                    result.getTotalPages(),
                    items
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
