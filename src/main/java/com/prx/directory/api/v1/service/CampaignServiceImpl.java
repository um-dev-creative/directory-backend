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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<CampaignTO> update(UUID id, CampaignUpdateRequest request) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign id is required");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        // Find existing campaign
        CampaignEntity existingCampaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));

        // Optimistic locking: check if last_update matches
        if (request.lastUpdate() != null) {
            if (!request.lastUpdate().equals(existingCampaign.getLastUpdate())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Campaign has been modified by another request. Please refresh and try again.");
            }
        }

        // Validate and update fields if provided
        boolean updated = false;

        if (request.name() != null) {
            existingCampaign.setName(request.name());
            updated = true;
        }

        if (request.description() != null) {
            existingCampaign.setDescription(request.description());
            updated = true;
        }

        if (request.active() != null) {
            existingCampaign.setActive(request.active());
            updated = true;
        }

        // Handle date updates with validation
        Instant newStartDate = request.startDate() != null ? request.startDate() : existingCampaign.getStartDate();
        Instant newEndDate = request.endDate() != null ? request.endDate() : existingCampaign.getEndDate();

        // Validate date constraint
        if (newStartDate.isAfter(newEndDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before or equal to endDate");
        }

        if (request.startDate() != null) {
            existingCampaign.setStartDate(request.startDate());
            updated = true;
        }

        if (request.endDate() != null) {
            existingCampaign.setEndDate(request.endDate());
            updated = true;
        }

        // Validate and update categoryId if provided
        if (request.categoryId() != null) {
            if (!categoryRepository.existsById(request.categoryId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
            }
            var categoryEntity = new com.prx.directory.jpa.entity.CategoryEntity();
            categoryEntity.setId(request.categoryId());
            existingCampaign.setCategoryFk(categoryEntity);
            updated = true;
        }

        // Validate and update businessId if provided
        if (request.businessId() != null) {
            if (!businessRepository.existsById(request.businessId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found");
            }
            var businessEntity = new com.prx.directory.jpa.entity.BusinessEntity();
            businessEntity.setId(request.businessId());
            existingCampaign.setBusinessFk(businessEntity);
            updated = true;
        }

        // Update last_update timestamp if any field was updated
        if (updated) {
            existingCampaign.setLastUpdate(Instant.now());
        }

        CampaignEntity saved = campaignRepository.save(existingCampaign);
        CampaignTO result = campaignMapper.toTO(saved);

        return ResponseEntity.ok(result);
    }
}
