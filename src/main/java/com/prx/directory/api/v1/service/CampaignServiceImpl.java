package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.CampaignListResponse;
import com.prx.directory.api.v1.to.CampaignTO;
import com.prx.directory.api.v1.to.CampaignUpdateRequest;
import com.prx.directory.api.v1.to.CampaignUpdateResponse;
import com.prx.directory.constant.DirectoryAppConstants;
import com.prx.directory.jpa.entity.CampaignEntity;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.jpa.repository.CampaignRepository;
import com.prx.directory.jpa.repository.CategoryRepository;
import com.prx.directory.jpa.spec.CampaignCriteria;
import com.prx.directory.jpa.spec.CampaignSpecifications;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class CampaignServiceImpl implements CampaignService {

    private static final int TERM_LENGTH = 2500;

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

        if (Objects.isNull(categoryId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DirectoryAppConstants.CAMPAIGN_CATEGORY_ID_REQUIRED);
        }
        if (Objects.isNull(businessId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DirectoryAppConstants.CAMPAIGN_BUSINESS_ID_REQUIRED);
        }

        if (!categoryRepository.existsById(categoryId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DirectoryAppConstants.CATEGORY_NOT_FOUND);
        }
        if (!businessRepository.existsById(businessId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DirectoryAppConstants.BUSINESS_NOT_FOUND);
        }

        CampaignEntity entity = campaignMapper.toEntity(campaignTO);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedDate(now);
        entity.setLastUpdate(now);
        if (Objects.isNull(entity.getActive())) {
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
        int p = (Objects.isNull(page) || page < 1) ? DirectoryAppConstants.DEFAULT_PAGE : page;
        int pp = (Objects.isNull(perPage) || perPage < 1) ? DirectoryAppConstants.DEFAULT_PER_PAGE : perPage;
        if (pp > DirectoryAppConstants.MAX_PER_PAGE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Sort sortObj = CampaignSortParser.parse(sort);
        if (Objects.isNull(sortObj)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            // Parse all filters using helper
            String name = filterParser.parseName(filters);
            UUID categoryId = filterParser.parseCategoryId(filters);
            UUID businessId = filterParser.parseBusinessId(filters);
            Boolean active = filterParser.parseActive(filters);

            // Parse instants and convert to UTC LocalDateTime for criteria
            Instant startFromI = filterParser.parseStartFrom(filters);
            Instant startToI = filterParser.parseStartTo(filters);
            Instant endFromI = filterParser.parseEndFrom(filters);
            Instant endToI = filterParser.parseEndTo(filters);

            LocalDateTime startFrom = startFromI == null ? null : LocalDateTime.ofInstant(startFromI, ZoneOffset.UTC);
            LocalDateTime startTo = startToI == null ? null : LocalDateTime.ofInstant(startToI, ZoneOffset.UTC);
            LocalDateTime endFrom = endFromI == null ? null : LocalDateTime.ofInstant(endFromI, ZoneOffset.UTC);
            LocalDateTime endTo = endToI == null ? null : LocalDateTime.ofInstant(endToI, ZoneOffset.UTC);

            // Validate date ranges
            filterParser.validateDateRanges(startFrom, startTo, endFrom, endTo);

            Pageable pageable = PageRequest.of(p - 1, pp, sortObj);
            var criteria = CampaignCriteria.of(active, startFrom, startTo, endFrom, endTo);
            Specification<CampaignEntity> spec = CampaignSpecifications
                    .byFilters(name, categoryId, businessId, criteria);
            Page<CampaignEntity> result = campaignRepository.findAll(spec, pageable);

            List<com.prx.directory.api.v1.to.CampaignResumeTO> items = result.getContent().stream().map(campaignMapper::toResumeTO)
                    .toList();

            // Compute counts for root-level distribution fields
            LocalDateTime now = LocalDateTime.now();
            // Use the same specification with additional predicates so counts respect filters
            // CampaignSpecifications.byFilters always returns a non-null Specification, so build
            // derived specifications by directly calling spec.and(...).
            Specification<CampaignEntity> activesSpec = spec.and((root, query, cb) -> cb.equal(root.get("status"), "ACTIVE"));
            Specification<CampaignEntity> inactivesSpec = spec.and((root, query, cb) -> cb.equal(root.get("status"), "INACTIVE"));
            Specification<CampaignEntity> expiredSpec = spec.and((root, query, cb) -> cb.lessThan(root.get("endDate"), now));

            long actives = campaignRepository.count(activesSpec);
            long inactives = campaignRepository.count(inactivesSpec);
            long expired = campaignRepository.count(expiredSpec);

            CampaignListResponse response = new CampaignListResponse(
                    result.getTotalElements(),
                    p,
                    pp,
                    result.getTotalPages(),
                    actives,
                    inactives,
                    expired,
                    items
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            // Return a BAD_REQUEST without a body to keep the generic type consistent
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Override
    @Transactional
    public ResponseEntity<CampaignUpdateResponse> update(UUID id, CampaignUpdateRequest request) {
        if (Objects.isNull(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DirectoryAppConstants.CAMPAIGN_ID_REQUIRED);
        }
        if (Objects.isNull(request)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DirectoryAppConstants.CAMPAIGN_REQUEST_BODY_REQUIRED);
        }

        // Find existing campaign
        CampaignEntity existingCampaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, DirectoryAppConstants.CAMPAIGN_NOT_FOUND));

        // Optimistic locking: check if last_update matches
        if (Objects.nonNull(request.lastUpdate()) && !request.lastUpdate().equals(existingCampaign.getLastUpdate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    DirectoryAppConstants.CAMPAIGN_MODIFIED_BY_ANOTHER_REQUEST);
        }

        boolean updated = false;
        updated |= applyBasicFieldUpdates(existingCampaign, request);
        updated |= applyDateUpdatesWithValidation(existingCampaign, request);
        updated |= applyRelationUpdates(existingCampaign, request);

        // Update last_update timestamp if any field was updated
        if (updated) {
            existingCampaign.setLastUpdate(LocalDateTime.now());
        }

        CampaignEntity saved = campaignRepository.save(existingCampaign);
        // Ensure category/business associations exist on returned instance (defensive for some JPA/mock behaviors)
        if (saved.getCategoryFk() == null && existingCampaign.getCategoryFk() != null) {
            saved.setCategoryFk(existingCampaign.getCategoryFk());
        }
        if (saved.getBusinessFk() == null && existingCampaign.getBusinessFk() != null) {
            saved.setBusinessFk(existingCampaign.getBusinessFk());
        }

        // Build minimal response and return 202 Accepted
        CampaignUpdateResponse resp = new CampaignUpdateResponse(saved.getId(), saved.getLastUpdate());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
    }

    // --- helpers to reduce cognitive complexity ---

    private boolean applyBasicFieldUpdates(CampaignEntity existingCampaign, CampaignUpdateRequest request) {
        boolean updated = false;
        if (Objects.nonNull(request.title())) {
            existingCampaign.setTitle(request.title());
            updated = true;
        }
        if (Objects.nonNull(request.description())) {
            existingCampaign.setDescription(request.description());
            updated = true;
        }
        if (Objects.nonNull(request.active())) {
            existingCampaign.setActive(request.active());
            updated = true;
        }
        // Apply discount if provided
        if (Objects.nonNull(request.discount())) {
            // Validate range defensively (DTO has annotations, but enforce here for service-level safety)
            if (request.discount().compareTo(BigDecimal.ZERO) < 0 || request.discount().compareTo(new BigDecimal("100")) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "discount must be between 0 and 100");
            }
            existingCampaign.setDiscount(request.discount());
            updated = true;
        }
        // Apply terms if provided
        if (Objects.nonNull(request.terms())) {
            if (request.terms().length() > TERM_LENGTH) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "terms must not exceed "
                        + TERM_LENGTH + " characters");
            }
            existingCampaign.setTerms(request.terms());
            updated = true;
        }
        return updated;
    }

    private boolean applyDateUpdatesWithValidation(CampaignEntity existingCampaign, CampaignUpdateRequest request) {
        boolean updated = false;
        // Handle date updates with validation
        LocalDateTime newStartDate = Objects.nonNull(request.startDate()) ? request.startDate() : existingCampaign.getStartDate();
        LocalDateTime newEndDate = Objects.nonNull(request.endDate()) ? request.endDate() : existingCampaign.getEndDate();

        // Validate date constraint
        if (Objects.nonNull(newStartDate) && Objects.nonNull(newEndDate) && newStartDate.isAfter(newEndDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DirectoryAppConstants.CAMPAIGN_START_DATE_AFTER_END_DATE);
        }

        if (Objects.nonNull(request.startDate())) {
            existingCampaign.setStartDate(request.startDate());
            updated = true;
        }

        if (Objects.nonNull(request.endDate())) {
            existingCampaign.setEndDate(request.endDate());
            updated = true;
        }
        return updated;
    }

    private boolean applyRelationUpdates(CampaignEntity existingCampaign, CampaignUpdateRequest request) {
        boolean updated = false;
        // Validate and update categoryId if provided
        if (Objects.nonNull(request.categoryId())) {
            if (!categoryRepository.existsById(request.categoryId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, DirectoryAppConstants.CATEGORY_NOT_FOUND);
            }
            var categoryEntity = new com.prx.directory.jpa.entity.CategoryEntity();
            categoryEntity.setId(request.categoryId());
            existingCampaign.setCategoryFk(categoryEntity);
            updated = true;
        }

        // Validate and update businessId if provided
        if (Objects.nonNull(request.businessId())) {
            if (!businessRepository.existsById(request.businessId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, DirectoryAppConstants.BUSINESS_NOT_FOUND);
            }
            var businessEntity = new com.prx.directory.jpa.entity.BusinessEntity();
            businessEntity.setId(request.businessId());
            existingCampaign.setBusinessFk(businessEntity);
            updated = true;
        }
        return updated;
    }
}
