package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.*;
import com.umdc.directory.constant.CampaignStatus;
import com.umdc.directory.constant.CampaignType;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CampaignEntity;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.CampaignRepository;
import com.umdc.directory.jpa.repository.CategoryRepository;
import com.umdc.directory.jpa.spec.CampaignCriteria;
import com.umdc.directory.jpa.spec.CampaignSpecifications;
import com.umdc.directory.mapper.CampaignMapper;
import com.umdc.directory.util.JwtUtil;
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
import java.util.Optional;
import java.util.UUID;

@Service
@SuppressWarnings("PMD.GodClass") // list, update and lifecycle live together; the helpers are already split out
public class CampaignServiceImpl implements CampaignService {

    private static final int TERM_LENGTH = 2500;

    private final CampaignRepository campaignRepository;
    private final CategoryRepository categoryRepository;
    private final BusinessRepository businessRepository;
    private final CampaignMapper campaignMapper;
    private final CampaignFilterParser filterParser;
    private final CampaignStatusCounter statusCounter;

    public CampaignServiceImpl(CampaignRepository campaignRepository,
                               CategoryRepository categoryRepository,
                               BusinessRepository businessRepository,
                               CampaignMapper campaignMapper,
                               CampaignStatusCounter statusCounter) {
        this.campaignRepository = campaignRepository;
        this.categoryRepository = categoryRepository;
        this.businessRepository = businessRepository;
        this.campaignMapper = campaignMapper;
        this.statusCounter = statusCounter;
        this.filterParser = new CampaignFilterParser();
    }

    @Override
    @Transactional
    public ResponseEntity<CampaignTO> create(CampaignTO campaignTO, String token) {
        UUID userId = JwtUtil.getUidFromToken(token);
        if (Objects.isNull(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
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
        var ownerId = businessRepository.findOwnerId(businessId);
        if (ownerId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!userId.equals(ownerId.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        CampaignType type = Objects.isNull(campaignTO.type()) ? CampaignType.PROMOTION : parseType(campaignTO.type());

        CampaignEntity entity = campaignMapper.toEntity(campaignTO);
        entity.setType(type.name());
        // Only DRAFT is persisted as status; every other status is computed from active and the dates
        entity.setStatus(CampaignStatus.STORED_DRAFT.equalsIgnoreCase(campaignTO.status()) ? CampaignStatus.STORED_DRAFT : null);
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
    @Transactional(readOnly = true)
    public ResponseEntity<CampaignTO> find(UUID id) {
        return campaignRepository.findById(id)
                .map(campaignMapper::toTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CampaignTO> find(UUID id, String token) {
        UUID userId = JwtUtil.getUidFromToken(token);
        return campaignRepository.findById(id)
                .filter(campaign -> isVisible(campaign, userId))
                .map(campaignMapper::toTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    private boolean isVisible(CampaignEntity campaign, UUID userId) {
        if (statusOf(campaign, LocalDateTime.now()).isPublic()) {
            return true;
        }
        if (Objects.isNull(userId) || Objects.isNull(campaign.getBusinessFk())) {
            return false;
        }
        return businessRepository.findOwnerId(campaign.getBusinessFk().getId()).filter(userId::equals).isPresent();
    }

    private static CampaignStatus statusOf(CampaignEntity campaign, LocalDateTime now) {
        return CampaignStatus.compute(campaign.getStatus(), campaign.getActive(), campaign.getStartDate(),
                campaign.getEndDate(), now);
    }

    private static CampaignType parseType(String type) {
        return CampaignType.parse(type)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, DirectoryAppConstants.CAMPAIGN_TYPE_INVALID));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CampaignListResponse> list(Integer page, Integer perPage, String sort, Map<String, String> filters) {
        return list(page, perPage, sort, filters, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CampaignListResponse> list(Integer page, Integer perPage, String sort,
                                                     Map<String, String> filters, String token) {
        UUID viewerId = JwtUtil.getUidFromToken(token);
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
            CampaignStatus statusFilter = filterParser.parseStatus(filters);
            String typeFilter = filterParser.parseType(filters);

            LocalDateTime listNow = LocalDateTime.now();
            Specification<CampaignEntity> spec = CampaignSpecifications
                    .byFilters(name, categoryId, businessId, criteria)
                    .and(CampaignSpecifications.visibleTo(viewerId, listNow));
            if (Objects.nonNull(statusFilter)) {
                spec = spec.and(CampaignSpecifications.byStatus(statusFilter, listNow));
            }
            if (Objects.nonNull(typeFilter)) {
                spec = spec.and(CampaignSpecifications.byType(typeFilter));
            }
            Page<CampaignEntity> result = campaignRepository.findAll(spec, pageable);

            List<CampaignResumeTO> items = result.getContent().stream().map(campaignMapper::toResumeTO)
                    .toList();

            long[] counts = statusCounter.countByStatus(spec, listNow);
            long actives   = counts[0];
            long inactives = counts[1];
            long expired   = counts[2];

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
    public ResponseEntity<CampaignUpdateResponse> update(UUID id, CampaignUpdateRequest request, String token) {
        UUID userId = JwtUtil.getUidFromToken(token);
        if (Objects.isNull(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (Objects.isNull(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DirectoryAppConstants.CAMPAIGN_ID_REQUIRED);
        }
        if (Objects.isNull(request)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DirectoryAppConstants.CAMPAIGN_REQUEST_BODY_REQUIRED);
        }

        // Find existing campaign
        CampaignEntity existingCampaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, DirectoryAppConstants.CAMPAIGN_NOT_FOUND));

        // Owner check on the business that owns the existing campaign
        var currentBusiness = existingCampaign.getBusinessFk();
        Optional<UUID> currentOwner = Objects.isNull(currentBusiness) ? Optional.empty()
                : businessRepository.findOwnerId(currentBusiness.getId());
        if (currentOwner.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, DirectoryAppConstants.BUSINESS_NOT_FOUND);
        }
        if (!userId.equals(currentOwner.get())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        // Moving the campaign to another business requires owning the target as well
        if (Objects.nonNull(request.businessId()) && !request.businessId().equals(currentBusiness.getId())) {
            Optional<UUID> targetOwner = businessRepository.findOwnerId(request.businessId());
            if (targetOwner.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, DirectoryAppConstants.BUSINESS_NOT_FOUND);
            }
            if (!userId.equals(targetOwner.get())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }

        // Optimistic locking: check if last_update matches
        if (Objects.nonNull(request.lastUpdate()) && !request.lastUpdate().equals(existingCampaign.getLastUpdate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    DirectoryAppConstants.CAMPAIGN_MODIFIED_BY_ANOTHER_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();
        CampaignStatus before = statusOf(existingCampaign, now);

        boolean updated = false;
        updated |= applyBasicFieldUpdates(existingCampaign, request);
        updated |= applyDateUpdatesWithValidation(existingCampaign, request);
        updated |= applyRelationUpdates(existingCampaign, request);
        updated |= applyType(existingCampaign, request);

        CampaignStatus after = statusOf(existingCampaign, now);
        if (before != after && !before.canTransitionTo(after)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    DirectoryAppConstants.CAMPAIGN_TRANSITION_NOT_ALLOWED + ": " + before + " -> " + after);
        }

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

    @Override
    @Transactional
    public ResponseEntity<CampaignTO> deactivate(UUID id, String token) {
        return transition(id, token, CampaignStatus.PAUSED);
    }

    @Override
    @Transactional
    public ResponseEntity<CampaignTO> reactivate(UUID id, String token) {
        return transition(id, token, CampaignStatus.ACTIVE);
    }

    @Override
    @Transactional
    public ResponseEntity<CampaignTO> publish(UUID id, String token) {
        return transition(id, token, CampaignStatus.SCHEDULED);
    }

    /// Owner-only lifecycle change. `intent` is PAUSED (deactivate), ACTIVE (reactivate) or SCHEDULED (publish).
    private ResponseEntity<CampaignTO> transition(UUID id, String token, CampaignStatus intent) {
        UUID userId = JwtUtil.getUidFromToken(token);
        if (Objects.isNull(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        CampaignEntity campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, DirectoryAppConstants.CAMPAIGN_NOT_FOUND));
        Optional<UUID> owner = Objects.isNull(campaign.getBusinessFk()) ? Optional.empty()
                : businessRepository.findOwnerId(campaign.getBusinessFk().getId());
        if (owner.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, DirectoryAppConstants.BUSINESS_NOT_FOUND);
        }
        if (!userId.equals(owner.get())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        LocalDateTime now = LocalDateTime.now();
        CampaignStatus current = statusOf(campaign, now);
        boolean publishing = intent == CampaignStatus.SCHEDULED;
        if (intent == CampaignStatus.ACTIVE) {
            requireStatus(current, CampaignStatus.PAUSED);
        } else if (publishing) {
            requireStatus(current, CampaignStatus.DRAFT);
        }
        boolean active = intent != CampaignStatus.PAUSED;
        String storedStatus = publishing ? noStoredStatus() : campaign.getStatus();
        CampaignStatus target = CampaignStatus.compute(storedStatus, active, campaign.getStartDate(), campaign.getEndDate(), now);
        if (!current.canTransitionTo(target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    DirectoryAppConstants.CAMPAIGN_TRANSITION_NOT_ALLOWED + ": " + current + " -> " + target);
        }
        campaign.setStatus(storedStatus);
        campaign.setActive(active);
        campaign.setLastUpdate(now);
        return ResponseEntity.ok(campaignMapper.toTO(campaignRepository.save(campaign)));
    }

    private static String noStoredStatus() {
        return null;
    }

    private static void requireStatus(CampaignStatus current, CampaignStatus expected) {
        if (current != expected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    DirectoryAppConstants.CAMPAIGN_TRANSITION_NOT_ALLOWED + ": campaign is " + current + ", expected " + expected);
        }
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

    private boolean applyType(CampaignEntity existingCampaign, CampaignUpdateRequest request) {
        if (Objects.isNull(request.type())) {
            return false;
        }
        existingCampaign.setType(parseType(request.type()).name());
        return true;
    }

    private boolean applyRelationUpdates(CampaignEntity existingCampaign, CampaignUpdateRequest request) {
        boolean updated = false;
        // Validate and update categoryId if provided
        if (Objects.nonNull(request.categoryId())) {
            if (!categoryRepository.existsById(request.categoryId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, DirectoryAppConstants.CATEGORY_NOT_FOUND);
            }
            var categoryEntity = new CategoryEntity();
            categoryEntity.setId(request.categoryId());
            existingCampaign.setCategoryFk(categoryEntity);
            updated = true;
        }

        // Validate and update businessId if provided
        if (Objects.nonNull(request.businessId())) {
            if (!businessRepository.existsById(request.businessId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, DirectoryAppConstants.BUSINESS_NOT_FOUND);
            }
            var businessEntity = new BusinessEntity();
            businessEntity.setId(request.businessId());
            existingCampaign.setBusinessFk(businessEntity);
            updated = true;
        }
        return updated;
    }
}
