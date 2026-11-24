package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.BusinessSearchItemTO;
import com.umdc.directory.api.v1.to.BusinessSearchResponse;
import com.umdc.directory.api.v1.to.LocationTO;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.LocationEntity;
import com.umdc.directory.jpa.entity.LocationOpeningHoursEntity;
import com.umdc.directory.jpa.entity.TimezoneEntity;
import com.umdc.directory.jpa.repository.LocationOpeningHoursRepository;
import com.umdc.directory.jpa.repository.TimezoneRepository;
import com.umdc.directory.util.OpeningHoursEvaluator;
import com.umdc.directory.util.OpeningHoursEvaluator.Slot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.umdc.directory.jpa.spec.SearchRankingSpecifications;
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

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("PMD.UseConcurrentHashMap") // the maps are local to one request
public class BusinessSearchServiceImpl implements BusinessSearchService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessSearchServiceImpl.class);
    private static final double EARTH_RADIUS_M = 6_371_000d;

    private final BusinessRepository businessRepository;
    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;
    private final LocationOpeningHoursRepository hoursRepository;
    private final TimezoneRepository timezoneRepository;
    private final Clock clock;

    @Autowired
    public BusinessSearchServiceImpl(BusinessRepository businessRepository, LocationRepository locationRepository,
                                     LocationMapper locationMapper, LocationOpeningHoursRepository hoursRepository,
                                     TimezoneRepository timezoneRepository) {
        this(businessRepository, locationRepository, locationMapper, hoursRepository, timezoneRepository, Clock.systemUTC());
    }

    /// For tests: "now" comes from the clock.
    BusinessSearchServiceImpl(BusinessRepository businessRepository, LocationRepository locationRepository,
                              LocationMapper locationMapper, LocationOpeningHoursRepository hoursRepository,
                              TimezoneRepository timezoneRepository, Clock clock) {
        this.businessRepository = businessRepository;
        this.locationRepository = locationRepository;
        this.locationMapper = locationMapper;
        this.hoursRepository = hoursRepository;
        this.timezoneRepository = timezoneRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("PMD.UseObjectForClearerAPI") // the parameters are the query string
    public ResponseEntity<BusinessSearchResponse> search(UUID cityId, String city, UUID categoryId, String text, Integer page, Integer perPage,
                                                         Boolean openNow, String lat, String lng, String sort) {
        int resolvedPage = page == null ? DirectoryAppConstants.DEFAULT_PAGE : page;
        int resolvedPerPage = perPage == null ? DirectoryAppConstants.DEFAULT_PER_PAGE : perPage;
        if (resolvedPage < 1 || resolvedPerPage < 1 || resolvedPerPage > DirectoryAppConstants.MAX_PER_PAGE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(DirectoryAppConstants.MESSAGE_ERROR_HEADER, DirectoryAppConstants.SEARCH_INVALID_PAGINATION).build();
        }
        GeoSearchValidator.SearchGeo geo;
        try {
            geo = GeoSearchValidator.searchGeo(lat, lng, sort);
        } catch (GeoSearchException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).header(DirectoryAppConstants.MESSAGE_ERROR_HEADER, e.getMessage()).build();
        }
        boolean onlyOpen = Boolean.TRUE.equals(openNow);
        // only counts and flags are logged: never the text of the search nor the coordinates
        logger.info("Searching businesses page={} perPage={} hasCity={} hasCategory={} hasText={} openNow={} byDistance={}", resolvedPage,
                resolvedPerPage, cityId != null || city != null, categoryId != null, text != null && !text.isBlank(), onlyOpen,
                geo.sortByDistance());

        Instant now = clock.instant();
        Specification<BusinessEntity> spec = BusinessSpecifications.from(new BusinessSearchCriteria(cityId, city, categoryId, text));
        if (onlyOpen) {
            spec = spec.and(openNowSpec(now));
        }
        Pageable pageable;
        if (geo.sortByDistance()) {
            // the order (distance, name, id) is part of the specification, evaluated by the database
            spec = spec.and(SearchRankingSpecifications.orderByDistance(geo.lat(), geo.lng()));
            pageable = PageRequest.of(resolvedPage - 1, resolvedPerPage);
        } else {
            // name, then id: a stable order so a business never moves between pages
            pageable = PageRequest.of(resolvedPage - 1, resolvedPerPage, Sort.by("name").ascending().and(Sort.by("id")));
        }
        // the database filters, orders and pages: the total is exact and nothing is cut
        var result = businessRepository.findAll(spec, pageable);
        return ResponseEntity.ok(response(result, enrich(result.getContent(), geo, now)));
    }

    /// The filter `open_now` for the database: the local day and time of `now` in every time zone in use, grouped, so that the
    /// comparison with the hours is made by SQL.
    private Specification<BusinessEntity> openNowSpec(Instant now) {
        List<TimezoneEntity> timezones = timezoneRepository.findAll();
        Map<String, Set<UUID>> ids = new HashMap<>();
        Map<String, int[]> day = new HashMap<>();
        Map<String, LocalTime> time = new HashMap<>();
        for (TimezoneEntity t : timezones) {
            group(ids, day, time, OpeningHoursServiceImpl.zoneOf(t), now).add(t.getId());
        }
        // businesses without a known zone use the default zone
        ZoneId fallback = OpeningHoursServiceImpl.zoneOf((TimezoneEntity) null);
        group(ids, day, time, fallback, now);
        String fallbackKey = key(fallback, now);
        List<SearchRankingSpecifications.OpenGroup> groups = new ArrayList<>();
        for (var e : ids.entrySet()) {
            groups.add(new SearchRankingSpecifications.OpenGroup(day.get(e.getKey())[0], time.get(e.getKey()), e.getValue(),
                    e.getKey().equals(fallbackKey)));
        }
        return SearchRankingSpecifications.openAt(groups, timezones.stream().map(TimezoneEntity::getId).collect(Collectors.toSet()));
    }

    private static String key(ZoneId zone, Instant now) {
        var local = now.atZone(zone);
        return local.getDayOfWeek().getValue() + "@" + local.toLocalTime().truncatedTo(ChronoUnit.SECONDS);
    }

    private static Set<UUID> group(Map<String, Set<UUID>> ids, Map<String, int[]> day, Map<String, LocalTime> time, ZoneId zone, Instant now) {
        var local = now.atZone(zone);
        String key = key(zone, now);
        day.putIfAbsent(key, new int[]{local.getDayOfWeek().getValue()});
        time.putIfAbsent(key, local.toLocalTime().truncatedTo(ChronoUnit.SECONDS));
        return ids.computeIfAbsent(key, k -> new HashSet<>());
    }

    private static BusinessSearchResponse response(Page<?> page, List<BusinessSearchItemTO> items) {
        return new BusinessSearchResponse(page.getTotalElements(), page.getNumber() + 1, page.getSize(), page.getTotalPages(), items);
    }

    /// Builds the items of the businesses with one query for their locations, one for the hours of those locations and one for the
    /// time zones: three queries whatever the number of businesses.
    private List<BusinessSearchItemTO> enrich(List<BusinessEntity> businesses, GeoSearchValidator.SearchGeo geo, Instant now) {
        if (businesses.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = businesses.stream().map(BusinessEntity::getId).toList();
        Map<UUID, List<LocationEntity>> locations = locationRepository.findByBusinessIdIn(ids).stream()
                .collect(Collectors.groupingBy(l -> l.getBusiness().getId()));
        Map<UUID, List<Slot>> slotsByLocation = slotsByLocation(locations);
        Map<UUID, ZoneId> zones = zones(businesses);

        List<BusinessSearchItemTO> items = new ArrayList<>(businesses.size());
        for (BusinessEntity business : businesses) {
            List<LocationEntity> own = locations.getOrDefault(business.getId(), List.of());
            items.add(new BusinessSearchItemTO(
                    business.getId(),
                    business.getName(),
                    business.getDescription(),
                    business.getCategoryFk().getId(),
                    business.getCategoryFk().getName(),
                    business.isVerified(),
                    business.getProfileImageRef(),
                    own.stream().map(locationMapper::toTO).toList(),
                    openNow(own, slotsByLocation, zones.get(business.getId()), now),
                    geo.lat() == null ? null : distance(own, geo.lat(), geo.lng())));
        }
        return items;
    }

    private Map<UUID, List<Slot>> slotsByLocation(Map<UUID, List<LocationEntity>> locations) {
        List<UUID> locationIds = locations.values().stream().flatMap(List::stream).map(LocationEntity::getId).filter(Objects::nonNull).toList();
        if (locationIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<Slot>> slots = new HashMap<>();
        for (LocationOpeningHoursEntity h : hoursRepository.findByLocationIdIn(locationIds)) {
            slots.computeIfAbsent(h.getLocationFk(), k -> new ArrayList<>())
                    .add(new Slot(DayOfWeek.of(h.getDayOfWeek()), h.getOpensAt(), h.getClosesAt(), h.isClosed()));
        }
        return slots;
    }

    /// Zone of each business; the ones without a time zone get the default zone.
    private Map<UUID, ZoneId> zones(List<BusinessEntity> businesses) {
        Set<UUID> timezoneIds = businesses.stream().map(BusinessEntity::getTimezoneFk).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, TimezoneEntity> timezones = timezoneIds.isEmpty() ? Map.of()
                : timezoneRepository.findAllById(timezoneIds).stream().collect(Collectors.toMap(TimezoneEntity::getId, t -> t));
        Map<UUID, ZoneId> zones = new HashMap<>();
        for (BusinessEntity b : businesses) {
            zones.put(b.getId(), OpeningHoursServiceImpl.zoneOf(b.getTimezoneFk() == null ? null : timezones.get(b.getTimezoneFk())));
        }
        return zones;
    }

    /// @return true when some location is open at `now`; false when it has hours but none is open; null when no location has hours
    private static Boolean openNow(List<LocationEntity> locations, Map<UUID, List<Slot>> slotsByLocation, ZoneId zone, Instant now) {
        boolean anyHours = false;
        for (LocationEntity location : locations) {
            List<Slot> slots = location.getId() == null ? null : slotsByLocation.get(location.getId());
            if (slots == null || slots.isEmpty()) {
                continue;
            }
            anyHours = true;
            if (OpeningHoursEvaluator.isOpenNow(slots, zone, now)) {
                return true;
            }
        }
        return anyHours ? Boolean.FALSE : null;
    }

    /// @return metres to the nearest location that has coordinates; null when none has
    private static Long distance(List<LocationEntity> locations, double lat, double lng) {
        return locations.stream()
                .filter(l -> l.getLatitude() != null && l.getLongitude() != null)
                .map(l -> haversineM(lat, lng, l.getLatitude().doubleValue(), l.getLongitude().doubleValue()))
                .min(Double::compare).map(Math::round).orElse(null);
    }

    /// Great-circle distance in metres; the same formula and earth radius as `LocationGeoRepository` (`nearby`).
    static double haversineM(double lat1, double lng1, double lat2, double lng2) {
        double a = Math.pow(Math.sin(Math.toRadians(lat2 - lat1) / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(Math.toRadians(lng2 - lng1) / 2), 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(Math.min(1.0, a)));
    }
}
