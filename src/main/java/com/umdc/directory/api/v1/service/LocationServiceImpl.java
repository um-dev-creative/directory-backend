package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.LocationCreateRequest;
import com.umdc.directory.api.v1.to.LocationTO;
import com.umdc.directory.api.v1.to.LocationUpdateRequest;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.LocationEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.CityRepository;
import com.umdc.directory.jpa.repository.LocationRepository;
import com.umdc.directory.mapper.LocationMapper;
import com.umdc.directory.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class LocationServiceImpl implements LocationService {

    private static final Logger logger = LoggerFactory.getLogger(LocationServiceImpl.class);

    private final LocationRepository locationRepository;
    private final BusinessRepository businessRepository;
    private final CityRepository cityRepository;
    private final LocationMapper locationMapper;

    public LocationServiceImpl(LocationRepository locationRepository, BusinessRepository businessRepository,
                               CityRepository cityRepository, LocationMapper locationMapper) {
        this.locationRepository = locationRepository;
        this.businessRepository = businessRepository;
        this.cityRepository = cityRepository;
        this.locationMapper = locationMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<LocationTO>> list(UUID businessId) {
        if (Objects.isNull(businessId) || !businessRepository.existsById(businessId)) {
            return error(HttpStatus.NOT_FOUND, DirectoryAppConstants.BUSINESS_NOT_FOUND);
        }
        return ResponseEntity.ok(locationRepository.findByBusinessId(businessId).stream().map(locationMapper::toTO).toList());
    }

    @Override
    @Transactional
    public ResponseEntity<LocationTO> create(UUID businessId, LocationCreateRequest request, String token) {
        Access<LocationTO> access = authorize(businessId, token);
        if (access.failure() != null) {
            return access.failure();
        }
        if (Objects.isNull(request)) {
            return error(HttpStatus.BAD_REQUEST, DirectoryAppConstants.LOCATION_REQUEST_BODY_REQUIRED);
        }
        if (Objects.isNull(request.cityId())) {
            return error(HttpStatus.BAD_REQUEST, DirectoryAppConstants.LOCATION_CITY_REQUIRED);
        }
        if (Objects.isNull(request.addressLine()) || request.addressLine().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, DirectoryAppConstants.LOCATION_ADDRESS_REQUIRED);
        }
        var invalid = LocationValidator.validateFields(request.name(), request.addressLine(), request.postalCode(), request.phone(),
                request.latitude(), request.longitude());
        if (invalid.isPresent()) {
            return error(HttpStatus.BAD_REQUEST, invalid.get());
        }
        var city = cityRepository.findById(request.cityId());
        if (city.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, DirectoryAppConstants.CITY_NOT_FOUND);
        }

        boolean first = locationRepository.countByBusinessId(businessId) == 0;
        boolean main = first || Boolean.TRUE.equals(request.main());
        if (main && !first) {
            demoteCurrentMain(businessId);
        }

        var now = LocalDateTime.now();
        var location = new LocationEntity();
        location.setBusiness(access.business());
        location.setCity(city.get());
        location.setName(LocationValidator.blankToNull(request.name()));
        location.setAddressLine(request.addressLine().trim());
        location.setPostalCode(LocationValidator.blankToNull(request.postalCode()));
        location.setPhone(LocationValidator.blankToNull(request.phone()));
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
        location.setMain(main);
        location.setCreatedDate(now);
        location.setLastUpdate(now);
        var saved = locationRepository.save(location);
        logger.info("Location {} created for business {}", saved.getId(), businessId);
        return ResponseEntity.status(HttpStatus.CREATED).body(locationMapper.toTO(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<LocationTO> update(UUID businessId, UUID locationId, LocationUpdateRequest request, String token) {
        Access<LocationTO> access = authorize(businessId, token);
        if (access.failure() != null) {
            return access.failure();
        }
        if (Objects.isNull(request)) {
            return error(HttpStatus.BAD_REQUEST, DirectoryAppConstants.LOCATION_REQUEST_BODY_REQUIRED);
        }
        var found = Objects.isNull(locationId) ? Optional.<LocationEntity>empty()
                : locationRepository.findByIdAndBusinessId(locationId, businessId);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, DirectoryAppConstants.LOCATION_NOT_FOUND);
        }
        var location = found.get();

        if (request.addressLine() != null && request.addressLine().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, DirectoryAppConstants.LOCATION_ADDRESS_REQUIRED);
        }
        var invalid = LocationValidator.validateFields(request.name(), request.addressLine(), request.postalCode(), request.phone(),
                request.latitude(), request.longitude());
        if (invalid.isPresent()) {
            return error(HttpStatus.BAD_REQUEST, invalid.get());
        }
        if (Boolean.FALSE.equals(request.main()) && location.isMain()) {
            return error(HttpStatus.BAD_REQUEST, DirectoryAppConstants.LOCATION_MAIN_CANNOT_BE_UNSET);
        }
        if (request.cityId() != null) {
            var city = cityRepository.findById(request.cityId());
            if (city.isEmpty()) {
                return error(HttpStatus.BAD_REQUEST, DirectoryAppConstants.CITY_NOT_FOUND);
            }
            location.setCity(city.get());
        }
        if (Boolean.TRUE.equals(request.main()) && !location.isMain()) {
            demoteCurrentMain(businessId);
            location.setMain(true);
        }
        if (request.name() != null) {
            location.setName(LocationValidator.blankToNull(request.name()));
        }
        if (request.addressLine() != null) {
            location.setAddressLine(request.addressLine().trim());
        }
        if (request.postalCode() != null) {
            location.setPostalCode(LocationValidator.blankToNull(request.postalCode()));
        }
        if (request.phone() != null) {
            location.setPhone(LocationValidator.blankToNull(request.phone()));
        }
        if (request.latitude() != null) {
            location.setLatitude(request.latitude());
            location.setLongitude(request.longitude());
        }
        location.setLastUpdate(LocalDateTime.now());
        var saved = locationRepository.save(location);
        return ResponseEntity.ok(locationMapper.toTO(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<Void> delete(UUID businessId, UUID locationId, String token) {
        Access<Void> access = authorize(businessId, token);
        if (access.failure() != null) {
            return access.failure();
        }
        var found = Objects.isNull(locationId) ? Optional.<LocationEntity>empty()
                : locationRepository.findByIdAndBusinessId(locationId, businessId);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, DirectoryAppConstants.LOCATION_NOT_FOUND);
        }
        boolean wasMain = found.get().isMain();
        locationRepository.delete(found.get());
        locationRepository.flush();
        if (wasMain) {
            // the oldest remaining location becomes the main one, so the business always has one
            locationRepository.findByBusinessId(businessId).stream().findFirst().ifPresent(next -> {
                next.setMain(true);
                next.setLastUpdate(LocalDateTime.now());
                locationRepository.save(next);
            });
        }
        logger.info("Location {} deleted from business {}", locationId, businessId);
        return ResponseEntity.noContent().build();
    }

    /// The caller must hold a session with a `uid` and be the owner of the business.
    private <T> Access<T> authorize(UUID businessId, String token) {
        var userId = JwtUtil.getUidFromToken(token);
        if (Objects.isNull(userId)) {
            return new Access<>(null, error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        var business = Objects.isNull(businessId) ? Optional.<BusinessEntity>empty() : businessRepository.findById(businessId);
        if (business.isEmpty()) {
            return new Access<>(null, error(HttpStatus.NOT_FOUND, DirectoryAppConstants.BUSINESS_NOT_FOUND));
        }
        var owner = business.get().getUserFk();
        if (Objects.isNull(owner) || !userId.equals(owner)) {
            return new Access<>(null, error(HttpStatus.FORBIDDEN, "Only the owner of the business can change its locations"));
        }
        return new Access<>(business.get(), null);
    }

    private void demoteCurrentMain(UUID businessId) {
        locationRepository.findMainByBusinessId(businessId).ifPresent(current -> {
            current.setMain(false);
            current.setLastUpdate(LocalDateTime.now());
            // flushed before the new main is written: at most one main per business is enforced by a unique index
            locationRepository.saveAndFlush(current);
        });
    }

    private static <T> ResponseEntity<T> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).header(DirectoryAppConstants.MESSAGE_ERROR_HEADER, message).build();
    }

    /// Result of the authorization: the business, or the response to answer with.
    private record Access<T>(BusinessEntity business, ResponseEntity<T> failure) {
    }
}
