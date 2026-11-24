package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.CityTO;
import com.umdc.directory.api.v1.to.LocationTO;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.entity.LocationEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.entity.LocationOpeningHoursEntity;
import com.umdc.directory.jpa.entity.TimezoneEntity;
import com.umdc.directory.jpa.repository.LocationOpeningHoursRepository;
import com.umdc.directory.jpa.repository.LocationRepository;
import com.umdc.directory.jpa.repository.TimezoneRepository;
import com.umdc.directory.mapper.LocationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessSearchServiceImplTest {

    @Mock
    private BusinessRepository businessRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private LocationMapper locationMapper;

    @Mock
    private LocationOpeningHoursRepository hoursRepository;
    @Mock
    private TimezoneRepository timezoneRepository;

    /// 2026-11-23 is a Monday; 15:00Z is 10:00 in Toronto (EST) and 16:00 in Madrid.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T15:00:00Z"), ZoneOffset.UTC);

    private BusinessSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BusinessSearchServiceImpl(businessRepository, locationRepository, locationMapper, hoursRepository, timezoneRepository, CLOCK);
    }

    private static BusinessEntity business(String name) {
        var category = new CategoryEntity();
        category.setId(UUID.randomUUID());
        category.setName("Food");
        var b = new BusinessEntity();
        b.setId(UUID.randomUUID());
        b.setName(name);
        b.setDescription("d");
        b.setCategoryFk(category);
        b.setVerified(true);
        b.setProfileImageRef("https://img/x.png");
        return b;
    }

    private static LocationEntity location(BusinessEntity business) {
        var l = new LocationEntity();
        l.setBusiness(business);
        return l;
    }

    @Test
    void invalidPaginationIsA400WithoutTouchingTheDatabase() {
        for (int[] args : new int[][]{{0, 10}, {-1, 10}, {1, 0}, {1, 101}}) {
            var response = service.search(null, null, null, null, args[0], args[1]);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(DirectoryAppConstants.SEARCH_INVALID_PAGINATION, response.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER));
        }
        verifyNoInteractions(businessRepository, locationRepository, hoursRepository, timezoneRepository);
    }

    @Test
    void defaultsToTheFirstPageOfTenAndOrdersByNameThenId() {
        when(businessRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        var response = service.search(null, null, null, null, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(businessRepository).findAll(any(Specification.class), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
        assertEquals("name: ASC,id: ASC", captor.getValue().getSort().toString());
        assertEquals(0, response.getBody().totalCount());
        assertTrue(response.getBody().data().isEmpty());
        verify(locationRepository, never()).findByBusinessIdIn(any());
    }

    @Test
    void returnsTheBusinessesWithTheirLocationsAndThePageMetadata() {
        var a = business("A");
        var b = business("B");
        var l1 = location(a);
        var l2 = location(a);
        var cityTo = new CityTO(UUID.randomUUID(), "Toronto", "Ontario", "CA");
        var to1 = new LocationTO(UUID.randomUUID(), a.getId(), cityTo, null, "1", null, null, null, null, true, null, null);
        var to2 = new LocationTO(UUID.randomUUID(), a.getId(), cityTo, null, "2", null, null, null, null, false, null, null);
        when(businessRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a, b), org.springframework.data.domain.PageRequest.of(1, 2), 5));
        when(locationRepository.findByBusinessIdIn(List.of(a.getId(), b.getId()))).thenReturn(List.of(l1, l2));
        when(locationMapper.toTO(l1)).thenReturn(to1);
        when(locationMapper.toTO(l2)).thenReturn(to2);

        var response = service.search(UUID.randomUUID(), "Toronto", UUID.randomUUID(), "pan", 2, 2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var body = response.getBody();
        assertEquals(5, body.totalCount());
        assertEquals(2, body.page());
        assertEquals(2, body.perPage());
        assertEquals(3, body.totalPages());
        var items = List.copyOf(body.data());
        assertEquals(List.of(to1, to2), items.get(0).locations());
        assertEquals(List.of(), items.get(1).locations());
        assertEquals("A", items.get(0).name());
        assertEquals("Food", items.get(0).categoryName());
        assertTrue(items.get(0).verified());
        assertEquals("https://img/x.png", items.get(0).profileImageRef());
    }

    // ---- open_now, distance_m, sort ----

    private static LocationEntity located(BusinessEntity business, String lat, String lng) {
        var l = location(business);
        l.setId(UUID.randomUUID());
        if (lat != null) {
            l.setLatitude(new BigDecimal(lat));
            l.setLongitude(new BigDecimal(lng));
        }
        return l;
    }

    private static LocationOpeningHoursEntity hours(LocationEntity location, int day, String from, String to, boolean closed) {
        var h = new LocationOpeningHoursEntity();
        h.setLocationFk(location.getId());
        h.setDayOfWeek(day);
        h.setOpensAt(from == null ? null : LocalTime.parse(from));
        h.setClosesAt(to == null ? null : LocalTime.parse(to));
        h.setClosed(closed);
        return h;
    }

    private static TimezoneEntity zone(String name) {
        var t = new TimezoneEntity();
        t.setId(UUID.randomUUID());
        t.setName(name);
        return t;
    }

    private void page(BusinessEntity... businesses) {
        when(businessRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(businesses), org.springframework.data.domain.PageRequest.of(0, 10), businesses.length));
    }

    @Test
    void openNowIsComputedInTheTimeZoneOfTheBusinessAndNullWithoutHours() {
        var toronto = business("Toronto");   // Monday 09:30-17:00 local: open at 10:00 EST
        var madrid = business("Madrid");     // Monday 09:30-17:00 local: 16:00 local is open too, so use a 08:00-15:00 slot: closed
        var noHours = business("NoHours");
        var noZone = business("NoZone");     // default zone
        var tz1 = zone("America/Toronto");
        var tz2 = zone("Europe/Madrid");
        toronto.setTimezoneFk(tz1.getId());
        madrid.setTimezoneFk(tz2.getId());
        var lt = located(toronto, null, null);
        var lm = located(madrid, null, null);
        var ln = located(noHours, null, null);
        var lz = located(noZone, null, null);
        page(toronto, madrid, noHours, noZone);
        when(locationRepository.findByBusinessIdIn(any())).thenReturn(List.of(lt, lm, ln, lz));
        when(hoursRepository.findByLocationIdIn(any())).thenReturn(List.of(
                hours(lt, 1, "09:30", "17:00", false), hours(lm, 1, "08:00", "15:00", false), hours(lz, 1, null, null, true)));
        when(timezoneRepository.findAllById(any())).thenReturn(List.of(tz1, tz2));

        var items = List.copyOf(service.search(null, null, null, null, null, null, null, null, null, null).getBody().data());

        assertEquals(Boolean.TRUE, items.get(0).openNow());
        assertEquals(Boolean.FALSE, items.get(1).openNow());
        assertNull(items.get(2).openNow());
        assertEquals(Boolean.FALSE, items.get(3).openNow());
        assertNull(items.get(0).distanceM());
    }

    @Test
    void aBusinessWithAnyOpenLocationIsOpen() {
        var b = business("Two");
        var closed = located(b, null, null);
        var open = located(b, null, null);
        page(b);
        when(locationRepository.findByBusinessIdIn(any())).thenReturn(List.of(closed, open));
        when(hoursRepository.findByLocationIdIn(any())).thenReturn(List.of(hours(closed, 1, null, null, true), hours(open, 1, "09:00", "23:59", false)));
        var item = service.search(null, null, null, null, null, null, null, null, null, null).getBody().data().iterator().next();
        assertEquals(Boolean.TRUE, item.openNow());
    }

    @Test
    void anOffsetTimeZoneIsUsedWhenItsNameIsNotAnIanaId() {
        var b = business("Offset");
        var tz = zone("not-a-zone");
        tz.setUtcOffset(java.time.Duration.ofHours(-5));
        b.setTimezoneFk(tz.getId());
        var l = located(b, null, null);
        page(b);
        when(locationRepository.findByBusinessIdIn(any())).thenReturn(List.of(l));
        when(hoursRepository.findByLocationIdIn(any())).thenReturn(List.of(hours(l, 1, "10:00", "11:00", false)));
        when(timezoneRepository.findAllById(any())).thenReturn(List.of(tz));
        var item = service.search(null, null, null, null, null, null, null, null, null, null).getBody().data().iterator().next();
        assertEquals(Boolean.TRUE, item.openNow());
    }

    @Test
    void aWholePageIsLoadedWithOneQueryPerKindNotOnePerBusiness() {
        var list = new java.util.ArrayList<BusinessEntity>();
        var locations = new java.util.ArrayList<LocationEntity>();
        var slots = new java.util.ArrayList<LocationOpeningHoursEntity>();
        var tz = zone("America/Toronto");
        for (int i = 0; i < 10; i++) {
            var b = business("B" + i);
            b.setTimezoneFk(tz.getId());
            var l = located(b, "43.65", "-79.38");
            list.add(b);
            locations.add(l);
            slots.add(hours(l, 1, "09:00", "17:00", false));
        }
        page(list.toArray(new BusinessEntity[0]));
        when(locationRepository.findByBusinessIdIn(any())).thenReturn(locations);
        when(hoursRepository.findByLocationIdIn(any())).thenReturn(slots);
        when(timezoneRepository.findAllById(any())).thenReturn(List.of(tz));

        var response = service.search(null, null, null, null, null, null, null, "43.65", "-79.38", null);

        assertEquals(10, response.getBody().data().size());
        verify(businessRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
        verify(locationRepository, times(1)).findByBusinessIdIn(any());
        verify(hoursRepository, times(1)).findByLocationIdIn(any());
        verify(timezoneRepository, times(1)).findAllById(any());
        verify(hoursRepository, never()).findByLocationId(any());
        verify(hoursRepository, never()).findTimezoneOfBusiness(any());
        verifyNoMoreInteractions(businessRepository, locationRepository, hoursRepository, timezoneRepository);
    }

    @Test
    void distanceIsTheNearestLocationInMetres() {
        var b = business("Near");
        var far = located(b, "43.70", "-79.38");
        var near = located(b, "43.651", "-79.38");
        var noCoords = located(b, null, null);
        var none = business("None");
        var lnone = located(none, null, null);
        page(b, none);
        when(locationRepository.findByBusinessIdIn(any())).thenReturn(List.of(far, near, noCoords, lnone));

        var items = List.copyOf(service.search(null, null, null, null, null, null, null, "43.65", "-79.38", null).getBody().data());

        assertEquals(111L, items.get(0).distanceM(), 2);
        assertNull(items.get(1).distanceM());
        assertEquals(0d, BusinessSearchServiceImpl.haversineM(10, 10, 10, 10));
        assertEquals(111_195d, BusinessSearchServiceImpl.haversineM(0, 0, 1, 0), 5);
    }

    @Test
    void sortByDistanceDelegatesOrderAndPagingToTheDatabaseWithoutACap() {
        var near = business("B-near");
        var far = business("A-far");
        var ln = located(near, "43.66", "-79.38");
        var lf = located(far, "44.0", "-79.38");
        when(businessRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(near, far), org.springframework.data.domain.PageRequest.of(1, 2), 1500));
        when(locationRepository.findByBusinessIdIn(any())).thenReturn(List.of(ln, lf));

        var body = service.search(null, null, null, null, 2, 2, null, "43.65", "-79.38", "DISTANCE").getBody();

        assertEquals(1500, body.totalCount());
        assertEquals(750, body.totalPages());
        assertEquals(List.of("B-near", "A-far"), body.data().stream().map(i -> i.name()).toList());
        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(businessRepository, times(1)).findAll(any(Specification.class), captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(2, captor.getValue().getPageSize());
        assertTrue(captor.getValue().getSort().isUnsorted());
    }

    @Test
    void openNowTrueAsksTheDatabaseForTheOpenBusinessesUsingTheZonesInUse() {
        var open = business("Open");
        var lo = located(open, null, null);
        var tz = zone("America/Toronto");
        var offset = zone("not-a-zone");
        offset.setUtcOffset(java.time.Duration.ofHours(-5));
        when(timezoneRepository.findAll()).thenReturn(List.of(tz, offset));
        when(businessRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(open), org.springframework.data.domain.PageRequest.of(0, 10), 1));
        when(locationRepository.findByBusinessIdIn(any())).thenReturn(List.of(lo));
        when(hoursRepository.findByLocationIdIn(any())).thenReturn(List.of(hours(lo, 1, "09:00", "20:00", false)));

        var body = service.search(null, null, null, null, null, null, true, null, null, null).getBody();

        assertEquals(1, body.totalCount());
        assertEquals(Boolean.TRUE, body.data().iterator().next().openNow());
        verify(timezoneRepository).findAll();
        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(businessRepository).findAll(any(Specification.class), captor.capture());
        assertEquals(10, captor.getValue().getPageSize());
        assertEquals("name: ASC,id: ASC", captor.getValue().getSort().toString());
    }

    @Test
    void openNowFalseIsNoFilter() {
        page(business("A"));
        var body = service.search(null, null, null, null, null, null, false, null, null, null).getBody();
        assertEquals(1, body.totalCount());
        verify(timezoneRepository, never()).findAll();
    }

    @Test
    void openNowWithNoResultsIsAnEmptyPage() {
        when(businessRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        var body = service.search(null, null, null, null, null, null, true, null, null, null).getBody();
        assertEquals(0, body.totalCount());
        assertTrue(body.data().isEmpty());
        verifyNoInteractions(locationRepository, hoursRepository);
    }

    @Test
    void invalidGeoParametersAreA400WithTheReason() {
        for (String[] args : new String[][]{{"43", null, null}, {null, "-79", null}, {"x", "1", null}, {"91", "1", null}, {"1", "181", null},
                {null, null, "distance"}, {"1", "1", "size"}}) {
            var response = service.search(null, null, null, null, null, null, null, args[0], args[1], args[2]);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER) != null);
        }
        verifyNoInteractions(businessRepository, locationRepository, hoursRepository, timezoneRepository);
    }

    @Test
    void sortByNameIsAcceptedWithOrWithoutAPoint() {
        page(business("A"));
        assertEquals(HttpStatus.OK, service.search(null, null, null, null, null, null, null, null, null, "name").getStatusCode());
        assertEquals(HttpStatus.OK, service.search(null, null, null, null, null, null, null, "1", "1", "name").getStatusCode());
    }

    @Test
    void theOldSixArgumentSearchDelegates() {
        page(business("A"));
        assertEquals(HttpStatus.OK, service.search(null, null, null, null, null, null).getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, new BusinessSearchService() { }.search(null, null, null, null, null, null).getStatusCode());
    }
}
