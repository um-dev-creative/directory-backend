package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.BusinessSearchItemTO;
import com.umdc.directory.api.v1.to.BusinessSearchResponse;
import com.umdc.directory.api.v1.to.LocationTO;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.LocationRepository;
import com.umdc.directory.jpa.spec.BusinessSearchCriteria;
import com.umdc.directory.jpa.spec.BusinessSpecifications;
import com.umdc.directory.mapper.LocationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BusinessSearchServiceImpl implements BusinessSearchService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessSearchServiceImpl.class);

    private final BusinessRepository businessRepository;
    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    public BusinessSearchServiceImpl(BusinessRepository businessRepository, LocationRepository locationRepository,
                                     LocationMapper locationMapper) {
        this.businessRepository = businessRepository;
        this.locationRepository = locationRepository;
        this.locationMapper = locationMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BusinessSearchResponse> search(UUID cityId, String city, UUID categoryId, String text, Integer page, Integer perPage) {
        int resolvedPage = page == null ? DirectoryAppConstants.DEFAULT_PAGE : page;
        int resolvedPerPage = perPage == null ? DirectoryAppConstants.DEFAULT_PER_PAGE : perPage;
        if (resolvedPage < 1 || resolvedPerPage < 1 || resolvedPerPage > DirectoryAppConstants.MAX_PER_PAGE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(DirectoryAppConstants.MESSAGE_ERROR_HEADER, DirectoryAppConstants.SEARCH_INVALID_PAGINATION).build();
        }
        logger.info("Searching businesses cityId={} city={} categoryId={} page={} perPage={}", cityId, city, categoryId, resolvedPage, resolvedPerPage);

        var spec = BusinessSpecifications.from(new BusinessSearchCriteria(cityId, city, categoryId, text));
        // name, then id: a stable order so a business never moves between pages
        var pageable = PageRequest.of(resolvedPage - 1, resolvedPerPage, Sort.by("name").ascending().and(Sort.by("id")));
        var result = businessRepository.findAll(spec, pageable);

        List<UUID> ids = result.getContent().stream().map(BusinessEntity::getId).toList();
        Map<UUID, List<LocationTO>> locationsByBusiness = ids.isEmpty() ? Map.of()
                : locationRepository.findByBusinessIdIn(ids).stream().collect(Collectors.groupingBy(
                        location -> location.getBusiness().getId(),
                        Collectors.mapping(locationMapper::toTO, Collectors.toList())));

        List<BusinessSearchItemTO> items = result.getContent().stream()
                .map(business -> new BusinessSearchItemTO(
                        business.getId(),
                        business.getName(),
                        business.getDescription(),
                        business.getCategoryFk().getId(),
                        business.getCategoryFk().getName(),
                        business.isVerified(),
                        business.getProfileImageRef(),
                        locationsByBusiness.getOrDefault(business.getId(), List.of())))
                .toList();

        return ResponseEntity.ok(new BusinessSearchResponse(result.getTotalElements(), result.getNumber() + 1, result.getSize(),
                result.getTotalPages(), items));
    }
}
