package com.umdc.directory.api.v1.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.umdc.directory.api.v1.to.BusinessSearchItemTO;
import com.umdc.directory.api.v1.to.BusinessSearchResponse;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.entity.CityEntity;
import com.umdc.directory.jpa.entity.LocationEntity;
import com.umdc.directory.jpa.entity.LocationOpeningHoursEntity;
import com.umdc.directory.jpa.entity.TimezoneEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.LocationOpeningHoursRepository;
import com.umdc.directory.jpa.repository.LocationRepository;
import com.umdc.directory.jpa.repository.TimezoneRepository;
import com.umdc.directory.mapper.LocationMapper;
import com.umdc.directory.util.OpeningHoursEvaluator;
import com.umdc.directory.util.OpeningHoursEvaluator.Slot;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The `open_now` filter and the order by distance run in the database (H2 in PostgreSQL mode): no cap, exact total, and the same
/// answer as `OpeningHoursEvaluator`.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:searchsqldb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=USER;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS directory_site\\\\;CREATE SCHEMA IF NOT EXISTS general\\\\;"
                + "CREATE ALIAS IF NOT EXISTS directory_site.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"\\\\;"
                + "CREATE ALIAS IF NOT EXISTS general.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=test"
})
class BusinessSearchSqlTest {

    private static final double LAT = 43.6532;
    private static final double LNG = -79.3832;

    @Autowired
    private EntityManager em;
    @Autowired
    private EntityManagerFactory emf;
    @Autowired
    private BusinessRepository businessRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private LocationOpeningHoursRepository hoursRepository;
    @Autowired
    private TimezoneRepository timezoneRepository;

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private CityEntity city;
    private CategoryEntity category;
    private final Map<UUID, List<Slot>> slotsOfBusiness = new java.util.HashMap<>();
    private final Map<UUID, ZoneId> zoneOfBusiness = new java.util.HashMap<>();
    private final List<BusinessEntity> all = new ArrayList<>();

    @BeforeEach
    void data() {
        var logger = (Logger) LoggerFactory.getLogger(BusinessSearchServiceImpl.class);
        logs.start();
        logger.addAppender(logs);

        city = new CityEntity();
        city.setName("Toronto");
        city.setProvince("Ontario");
        city.setCountry("CA");
        city.setCreatedDate(LocalDateTime.now());
        city.setLastUpdate(LocalDateTime.now());
        em.persist(city);
        category = new CategoryEntity();
        category.setName("Food");
        category.setDescription("Food");
        category.setActive(true);
        category.setCreatedDate(LocalDateTime.now());
        category.setLastUpdate(LocalDateTime.now());
        em.persist(category);

        var toronto = zone("America/Toronto", Duration.ofHours(-5));
        var madrid = zone("Europe/Madrid", Duration.ofHours(1));
        var tokyo = zone("Asia/Tokyo", Duration.ofHours(9));
        var offset = zone("not-a-zone", Duration.ofHours(-5));
        var garbage = zone("also-bad", null);
        var unknownZone = UUID.randomUUID();

        // every business has the same weekly pattern per day so that some instant of the week hits each branch
        var daytime = List.of(slot(1, "09:00", "17:00"), slot(2, "09:00", "17:00"), slot(3, "09:00", "17:00"), slot(4, "09:00", "17:00"),
                slot(5, "09:00", "23:59"), slot(6, "00:00", "02:00"), closedSlot(7));
        var night = List.of(slot(1, "18:00", "23:59"), slot(2, "00:00", "03:00"), slot(2, "18:00", "23:59"), slot(3, "00:00", "03:00"));
        var split = List.of(slot(1, "08:00", "12:00"), slot(1, "14:00", "18:00"), slot(6, "10:00", "20:00"), slot(7, "10:00", "20:00"));
        var batch = List.of(daytime, night, split);
        int n = 0;
        for (var tz : new UUID[]{toronto, madrid, tokyo, offset, garbage, unknownZone, null}) {
            for (var slots : batch) {
                business("B" + n++, tz, slots);
            }
        }
        business("NoHours", toronto, List.of());
        em.flush();
        em.clear();
    }

    @AfterEach
    void detach() {
        ((Logger) LoggerFactory.getLogger(BusinessSearchServiceImpl.class)).detachAppender(logs);
    }

    private UUID zone(String name, Duration offset) {
        var t = new TimezoneEntity();
        t.setId(UUID.randomUUID());
        t.setName(name);
        t.setUtcOffset(offset == null ? Duration.ZERO : offset);
        t.setAbbreviation("TZ");
        t.setCreatedAt(LocalDateTime.now());
        t.setLastUpdated(LocalDateTime.now());
        em.persist(t);
        zones.put(t.getId(), OpeningHoursServiceImpl.zoneOf(t));
        return t.getId();
    }

    private final Map<UUID, ZoneId> zones = new java.util.HashMap<>();

    private static Slot slot(int day, String from, String to) {
        return new Slot(DayOfWeek.of(day), LocalTime.parse(from), LocalTime.parse(to), false);
    }

    private static Slot closedSlot(int day) {
        return new Slot(DayOfWeek.of(day), null, null, true);
    }

    private BusinessEntity business(String name, UUID tz, List<Slot> slots) {
        var b = new BusinessEntity();
        b.setName(name);
        b.setCategoryFk(category);
        b.setUserFk(UUID.randomUUID());
        b.setTimezoneFk(tz);
        b.setCreatedDate(LocalDateTime.now());
        b.setLastUpdate(LocalDateTime.now());
        em.persist(b);
        var l = new LocationEntity();
        l.setBusiness(b);
        l.setCity(city);
        l.setAddressLine("1 Main St");
        l.setMain(true);
        l.setLatitude(BigDecimal.valueOf(LAT + all.size() * 0.001));
        l.setLongitude(BigDecimal.valueOf(LNG));
        l.setCreatedDate(LocalDateTime.now());
        l.setLastUpdate(LocalDateTime.now());
        em.persist(l);
        for (var s : slots) {
            em.persist(new LocationOpeningHoursEntity(l.getId(), s.day().getValue(), s.opens(), s.closes(), s.closed()));
        }
        slotsOfBusiness.put(b.getId(), slots);
        zoneOfBusiness.put(b.getId(), tz == null || !zones.containsKey(tz) ? OpeningHoursServiceImpl.zoneOf((TimezoneEntity) null) : zones.get(tz));
        all.add(b);
        return b;
    }

    private BusinessSearchServiceImpl service(Instant at) {
        return new BusinessSearchServiceImpl(businessRepository, locationRepository, Mockito.mock(LocationMapper.class), hoursRepository,
                timezoneRepository, Clock.fixed(at, ZoneOffset.UTC));
    }

    private static Set<String> names(BusinessSearchResponse r) {
        return r.data().stream().map(BusinessSearchItemTO::name).collect(Collectors.toSet());
    }

    @Test
    void openNowInSqlEqualsTheJavaEvaluatorAtEveryInstantOfTheWeek() {
        Instant start = Instant.parse("2026-11-22T00:00:00Z");
        int checked = 0;
        int nonEmpty = 0;
        for (Instant at = start; at.isBefore(start.plus(Duration.ofDays(8))); at = at.plus(Duration.ofMinutes(37))) {
            Set<String> expected = new HashSet<>();
            for (var b : all) {
                var slots = slotsOfBusiness.get(b.getId());
                if (!slots.isEmpty() && OpeningHoursEvaluator.isOpenNow(slots, zoneOfBusiness.get(b.getId()), at)) {
                    expected.add(b.getName());
                }
            }
            var response = service(at).search(null, null, null, null, 1, 100, true, null, null, null).getBody();
            assertEquals(expected, names(response), "at " + at);
            assertEquals(expected.size(), response.totalCount(), "total at " + at);
            assertTrue(response.data().stream().allMatch(i -> Boolean.TRUE.equals(i.openNow())));
            checked++;
            nonEmpty += expected.isEmpty() ? 0 : 1;
        }
        assertTrue(checked > 300);
        assertTrue(nonEmpty > 100, "the sample must open and close");
    }

    @Test
    void openNowIsExactBeyondAThousandBusinessesAndPagesWithoutCap() {
        // 1100 extra open businesses: the old in-memory path stopped at 1000
        for (int i = 0; i < 1100; i++) {
            business(String.format("Z%04d", i), null, List.of(slot(1, "00:00", "23:59")));
        }
        em.flush();
        em.clear();
        Instant monday = Instant.parse("2026-11-23T15:00:00Z");
        var first = service(monday).search(null, null, null, null, 1, 100, true, null, null, null).getBody();
        assertTrue(first.totalCount() > 1100);
        assertEquals(100, first.data().size());
        long total = first.totalCount();
        int lastPage = (int) ((total + 99) / 100);
        var last = service(monday).search(null, null, null, null, lastPage, 100, true, null, null, null).getBody();
        assertEquals(total - (lastPage - 1) * 100L, last.data().size());
        assertEquals(lastPage, last.totalPages());
        assertTrue(service(monday).search(null, null, null, null, lastPage + 1, 100, true, null, null, null).getBody().data().isEmpty());
    }

    @Test
    void sortByDistanceIsDoneByTheDatabaseNearestFirstAndNoCoordinatesLast() {
        var far = business("Far", null, List.of());
        var noCoords = business("NoCoords", null, List.of());
        var near = business("A0Near", null, List.of());
        em.flush();
        for (var b : List.of(far, near)) {
            var l = locationRepository.findByBusinessId(b.getId()).getFirst();
            l.setLatitude(BigDecimal.valueOf(b == near ? LAT : LAT + 2));
            l.setLongitude(BigDecimal.valueOf(LNG));
        }
        var lNo = locationRepository.findByBusinessId(noCoords.getId()).getFirst();
        lNo.setLatitude(null);
        lNo.setLongitude(null);
        em.flush();
        em.clear();

        var page1 = service(Instant.parse("2026-11-23T15:00:00Z")).search(null, null, null, null, 1, 5, null, String.valueOf(LAT), String.valueOf(LNG), "distance").getBody();
        var names = page1.data().stream().map(BusinessSearchItemTO::name).toList();
        assertEquals("A0Near", names.getFirst());
        long total = page1.totalCount();
        assertEquals(all.size(), total);
        var lastPage = service(Instant.parse("2026-11-23T15:00:00Z")).search(null, null, null, null, (int) total, 1, null, String.valueOf(LAT), String.valueOf(LNG), "distance").getBody();
        assertEquals("NoCoords", lastPage.data().iterator().next().name());
        // distances never decrease along the pages
        long previous = -1;
        for (int p = 1; p <= (total + 9) / 10; p++) {
            var body = service(Instant.parse("2026-11-23T15:00:00Z")).search(null, null, null, null, p, 10, null, String.valueOf(LAT), String.valueOf(LNG), "distance").getBody();
            for (var item : body.data()) {
                long d = item.distanceM() == null ? Long.MAX_VALUE : item.distanceM();
                assertTrue(d >= previous, item.name());
                previous = d;
            }
        }
    }

    @Test
    void distanceAndOpenNowCombineAndTheTotalIsExact() {
        Instant monday = Instant.parse("2026-11-23T15:00:00Z");
        var body = service(monday).search(null, null, null, null, 1, 100, true, String.valueOf(LAT), String.valueOf(LNG), "distance").getBody();
        var plain = service(monday).search(null, null, null, null, 1, 100, true, null, null, null).getBody();
        assertEquals(plain.totalCount(), body.totalCount());
        assertEquals(names(plain), names(body));
        assertFalse(body.data().isEmpty());
    }

    @Test
    void aSearchRunsAFixedNumberOfQueriesWhateverThePage() {
        SessionFactory sf = emf.unwrap(SessionFactory.class);
        sf.getStatistics().setStatisticsEnabled(true);
        Instant monday = Instant.parse("2026-11-23T15:00:00Z");
        service(monday).search(null, null, null, null, 1, 5, true, String.valueOf(LAT), String.valueOf(LNG), "distance");
        sf.getStatistics().clear();
        var small = service(monday).search(null, null, null, null, 1, 2, true, String.valueOf(LAT), String.valueOf(LNG), "distance").getBody();
        long smallQueries = sf.getStatistics().getPrepareStatementCount();
        sf.getStatistics().clear();
        var big = service(monday).search(null, null, null, null, 1, 5, true, String.valueOf(LAT), String.valueOf(LNG), "distance").getBody();
        long bigQueries = sf.getStatistics().getPrepareStatementCount();
        assertTrue(big.data().size() > small.data().size());
        assertEquals(smallQueries, bigQueries);
        // timezones, page, count, locations, hours, timezones of the page
        assertTrue(bigQueries <= 6, "queries: " + bigQueries);
    }

    @Test
    void theSearchLogNeverHoldsTheTextNorTheCoordinates() {
        service(Instant.parse("2026-11-23T15:00:00Z")).search(UUID.randomUUID(), "SecretCityName", UUID.randomUUID(), "SecretQueryText", 1, 5, true,
                "43.123456", "-79.654321", "distance");
        assertFalse(logs.list.isEmpty());
        for (var event : logs.list) {
            String line = event.getFormattedMessage();
            assertFalse(line.contains("SecretQueryText"), line);
            assertFalse(line.contains("SecretCityName"), line);
            assertFalse(line.contains("43.123456"), line);
            assertFalse(line.contains("-79.654321"), line);
            assertTrue(line.contains("hasText=true"), line);
        }
    }

    @Test
    void anEmptyGroupListMatchesNothing() {
        assertEquals(0, businessRepository.count(com.umdc.directory.jpa.spec.SearchRankingSpecifications.openAt(List.of(), Set.of())));
    }

    @Test
    void withoutTimezoneRowsEveryBusinessUsesTheDefaultZone() {
        Instant monday = Instant.parse("2026-11-23T15:00:00Z"); // 10:00 in Toronto
        em.createQuery("update BusinessEntity b set b.timezoneFk = null").executeUpdate();
        em.createQuery("delete from TimezoneEntity").executeUpdate();
        em.flush();
        em.clear();
        var body = service(monday).search(null, null, null, null, 1, 100, true, null, null, null).getBody();
        for (var b : all) {
            var slots = slotsOfBusiness.get(b.getId());
            boolean open = !slots.isEmpty() && OpeningHoursEvaluator.isOpenNow(slots, ZoneId.of("America/Toronto"), monday);
            assertEquals(open, names(body).contains(b.getName()), b.getName());
        }
    }
}
