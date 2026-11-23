package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.entity.CityEntity;
import com.umdc.directory.jpa.entity.LocationEntity;
import com.umdc.directory.jpa.entity.UserEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Runs the real Haversine, bbox and cluster SQL against H2 in PostgreSQL mode.
@DataJpaTest
@Import(LocationGeoRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:geodb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=USER;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS directory_site\\\\;CREATE SCHEMA IF NOT EXISTS general\\\\;"
                + "CREATE ALIAS IF NOT EXISTS directory_site.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"\\\\;"
                + "CREATE ALIAS IF NOT EXISTS general.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=test"
})
class LocationGeoRepositoryTest {

    private static final double LAT = 43.6532;
    private static final double LNG = -79.3832;

    @Autowired
    private EntityManager em;
    @Autowired
    private LocationGeoRepository repository;

    private CityEntity toronto;
    private CityEntity ottawa;
    private CategoryEntity food;
    private CategoryEntity bakery;
    private CategoryEntity services;
    private UserEntity owner;

    @BeforeEach
    void data() {
        owner = new UserEntity();
        em.persist(owner);
        toronto = city("Toronto");
        ottawa = city("Ottawa");
        food = category("Food", null);
        bakery = category("Bakery", food);
        services = category("Services", null);

        var zeta = business("Zeta Bakery", "Pan 100% casero_fresco", bakery, true);
        var alfa = business("Alfa Taqueria", "Tacos", food, false);
        var legal = business("Legal", null, services, false);
        var noCoords = business("No Coords", null, services, false);

        location(zeta, toronto, LAT, LNG, true);              // 0 m
        location(alfa, toronto, LAT, LNG, true);              // 0 m, tie with zeta
        location(alfa, toronto, LAT + 0.01, LNG, false);      // ~1.1 km
        location(legal, toronto, LAT + 0.1, LNG, true);       // ~11 km
        location(legal, ottawa, 45.4215, -75.6972, false);    // ~350 km
        location(noCoords, toronto, null, null, true);
        em.flush();
        em.clear();
    }

    @Test
    void radiusKeepsOnlyTheLocationsInside() {
        assertEquals(3, repository.countNearby(LAT, LNG, 5000, null, null, null));
        assertEquals(4, repository.countNearby(LAT, LNG, 15000, null, null, null));
        assertEquals(2, repository.countNearby(LAT, LNG, 100, null, null, null));
    }

    @Test
    void locationsWithoutCoordinatesAreIgnored() {
        var names = repository.findNearby(LAT, LNG, 50000, null, null, null, false, 50, 0).stream().map(r -> r.name()).toList();
        assertTrue(names.stream().noneMatch("No Coords"::equals));
        assertEquals(4, names.size());
    }

    @Test
    void sortedByDistanceWithStableTieBreakById() {
        var rows = repository.findNearby(LAT, LNG, 15000, null, null, null, false, 50, 0);
        assertEquals(4, rows.size());
        assertEquals(0d, rows.get(0).distanceM(), 0.5);
        assertEquals(0d, rows.get(1).distanceM(), 0.5);
        // identical distance: the lower id (hex order) goes first
        assertTrue(rows.get(0).locationId().toString().compareTo(rows.get(1).locationId().toString()) < 0);
        assertEquals(1112d, rows.get(2).distanceM(), 10);
        assertEquals(11119d, rows.get(3).distanceM(), 60);
        assertEquals(rows, repository.findNearby(LAT, LNG, 15000, null, null, null, false, 50, 0));
    }

    @Test
    void sortedByName() {
        var names = repository.findNearby(LAT, LNG, 15000, null, null, null, true, 50, 0).stream().map(r -> r.name()).toList();
        assertEquals(List.of("Alfa Taqueria", "Alfa Taqueria", "Legal", "Zeta Bakery"), names);
    }

    @Test
    void paginationSlicesTheOrderedResult() {
        var all = repository.findNearby(LAT, LNG, 15000, null, null, null, false, 50, 0);
        assertEquals(all.subList(1, 3), repository.findNearby(LAT, LNG, 15000, null, null, null, false, 2, 1));
        assertTrue(repository.findNearby(LAT, LNG, 15000, null, null, null, false, 2, 10).isEmpty());
    }

    @Test
    void categoryIncludesSubcategories() {
        assertEquals(3, repository.countNearby(LAT, LNG, 15000, food.getId(), null, null));
        assertEquals(1, repository.countNearby(LAT, LNG, 15000, bakery.getId(), null, null));
        assertEquals(1, repository.countNearby(LAT, LNG, 15000, services.getId(), null, null));
    }

    @Test
    void cityFilterAppliesToTheLocation() {
        assertEquals(1, repository.countNearby(45.42, -75.69, 5000, null, ottawa.getId(), null));
        assertEquals(0, repository.countNearby(LAT, LNG, 15000, null, ottawa.getId(), null));
        assertEquals(4, repository.countNearby(LAT, LNG, 15000, null, toronto.getId(), null));
    }

    @Test
    void textMatchesNameOrDescriptionAndEscapesWildcards() {
        assertEquals(1, repository.countNearby(LAT, LNG, 15000, null, null, "zeta"));
        assertEquals(2, repository.countNearby(LAT, LNG, 15000, null, null, "TACOS"));
        assertEquals(1, repository.countNearby(LAT, LNG, 15000, null, null, "100% casero_"));
        assertEquals(0, repository.countNearby(LAT, LNG, 15000, null, null, "z%"));
        assertEquals(0, repository.countNearby(LAT, LNG, 15000, null, null, "_eta"));
        assertEquals(1, repository.countNearby(LAT, LNG, 15000, null, null, "%"));
    }

    @Test
    void combinedFilters() {
        assertEquals(1, repository.countNearby(LAT, LNG, 15000, food.getId(), toronto.getId(), "pan"));
    }

    @Test
    void bboxReturnsThePointsInside() {
        var rows = repository.findInBbox(LNG - 0.05, LAT - 0.05, LNG + 0.05, LAT + 0.05, null, null, null, 10);
        assertEquals(3, rows.size());
        var limited = repository.findInBbox(LNG - 0.05, LAT - 0.05, LNG + 0.05, LAT + 0.05, null, null, null, 2);
        assertEquals(2, limited.size());
        assertEquals(1, repository.findInBbox(LNG - 0.05, LAT - 0.05, LNG + 0.05, LAT + 0.05, bakery.getId(), toronto.getId(), "zeta", 10).size());
        var first = rows.get(0);
        assertEquals(LAT, first.latitude(), 0.2);
    }

    @Test
    void clustersGroupByGridCell() {
        // 1 degree cells: the three Toronto-area points fall in one cell, Ottawa in another
        var clusters = repository.findClusters(-80, 43, -75, 46, null, null, null, 1d, 10);
        assertEquals(2, clusters.size());
        assertEquals(4, clusters.get(0).count());
        assertEquals(1, clusters.get(1).count());
        assertEquals(1, repository.findClusters(-80, 43, -75, 46, null, null, null, 1d, 1).size());
        // fine cells split the Toronto group
        assertTrue(repository.findClusters(-80, 43, -75, 46, null, null, null, 0.001d, 10).size() >= 4);
    }

    @Test
    void nearTheAntimeridianAndThePolesTheQueriesStillRun() {
        assertEquals(0, repository.countNearby(0, 179.9, 50000, null, null, null));
        assertEquals(0, repository.countNearby(0, -179.9, 50000, null, null, null));
        assertEquals(0, repository.countNearby(89.9999, 0, 50000, null, null, null));
        assertEquals(0, repository.countNearby(-89.9999, 0, 50000, null, null, null));
    }

    private CityEntity city(String name) {
        var c = new CityEntity();
        c.setName(name);
        c.setProvince("Ontario");
        c.setCountry("CA");
        c.setCreatedDate(LocalDateTime.now());
        c.setLastUpdate(LocalDateTime.now());
        em.persist(c);
        return c;
    }

    private CategoryEntity category(String name, CategoryEntity parent) {
        var c = new CategoryEntity();
        c.setName(name);
        c.setDescription(name);
        c.setCategoryParentFk(parent);
        c.setActive(true);
        c.setCreatedDate(LocalDateTime.now());
        c.setLastUpdate(LocalDateTime.now());
        em.persist(c);
        return c;
    }

    private BusinessEntity business(String name, String description, CategoryEntity category, boolean verified) {
        var b = new BusinessEntity();
        b.setName(name);
        b.setDescription(description);
        b.setCategoryFk(category);
        b.setUserFk(owner);
        b.setVerified(verified);
        b.setCreatedDate(LocalDateTime.now());
        b.setLastUpdate(LocalDateTime.now());
        em.persist(b);
        return b;
    }

    private void location(BusinessEntity business, CityEntity city, Double lat, Double lng, boolean main) {
        var l = new LocationEntity();
        l.setBusiness(business);
        l.setCity(city);
        l.setAddressLine("1 Main St");
        l.setMain(main);
        l.setLatitude(lat == null ? null : BigDecimal.valueOf(lat));
        l.setLongitude(lng == null ? null : BigDecimal.valueOf(lng));
        l.setCreatedDate(LocalDateTime.now());
        l.setLastUpdate(LocalDateTime.now());
        em.persist(l);
    }
}
