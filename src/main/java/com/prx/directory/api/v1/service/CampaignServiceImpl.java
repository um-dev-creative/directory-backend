package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.CampaignListResponse;
import com.prx.directory.api.v1.to.CampaignTO;
import com.prx.directory.api.v1.to.CampaignUpdateRequest;
import com.prx.directory.api.v1.to.OfferTO;
import com.prx.directory.constant.DirectoryAppConstants;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class CampaignServiceImpl implements CampaignService {


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
            // Return a BAD_REQUEST without a body to keep the generic type consistent
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Override
    @Transactional
    public ResponseEntity<CampaignTO> update(UUID id, CampaignUpdateRequest request) {
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
        CampaignTO result = campaignMapper.toTO(saved);

        return ResponseEntity.ok(result);
    }

    // --- helpers to reduce cognitive complexity ---

    private boolean applyBasicFieldUpdates(CampaignEntity existingCampaign, CampaignUpdateRequest request) {
        boolean updated = false;
        if (Objects.nonNull(request.name())) {
            existingCampaign.setName(request.name());
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
