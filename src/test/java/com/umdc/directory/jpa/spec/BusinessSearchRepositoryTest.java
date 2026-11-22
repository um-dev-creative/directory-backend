package com.umdc.directory.jpa.spec;

import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.entity.CityEntity;
import com.umdc.directory.jpa.entity.LocationEntity;
import com.umdc.directory.jpa.entity.UserEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.CityRepository;
import com.umdc.directory.jpa.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Runs the real queries (search specifications, location queries) against H2 in PostgreSQL mode: mocks cannot tell whether
/// the `EXISTS` sub-query, the category/parent join or the `LIKE ... ESCAPE` are right.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:searchdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=USER;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS directory_site\\\\;CREATE SCHEMA IF NOT EXISTS general\\\\;"
                + "CREATE ALIAS IF NOT EXISTS directory_site.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"\\\\;"
                + "CREATE ALIAS IF NOT EXISTS general.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=test"
})
class BusinessSearchRepositoryTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private BusinessRepository businessRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private CityRepository cityRepository;

    private CityEntity toronto;
    private CityEntity ottawa;
    private CategoryEntity food;
    private CategoryEntity bakery;
    private CategoryEntity services;
    private BusinessEntity panaderia;
    private BusinessEntity taqueria;
    private BusinessEntity abogado;
    private BusinessEntity sinSede;
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

        panaderia = business("Panadería La Estrella", "Pan 100% casero_fresco", bakery);
        taqueria = business("Taquería El Sol", "Tacos al pastor", food);
        abogado = business("Abogado Pérez", null, services);
        sinSede = business("Sin sede", "Sin sedes aún", services);

        location(panaderia, toronto, true);
        location(panaderia, ottawa, false);
        location(taqueria, toronto, true);
        location(abogado, ottawa, true);
        em.flush();
        em.clear();
    }

    private CityEntity city(String name) {
        var c = new CityEntity();
        c.setName(name);
        c.setProvince("Ontario");
        c.setCountry("CA");
        c.setCreatedDate(LocalDateTime.now());
        c.setLastUpdate(LocalDateTime.now());
        return cityRepository.save(c);
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

    private BusinessEntity business(String name, String description, CategoryEntity category) {
        var b = new BusinessEntity();
        b.setName(name);
        b.setDescription(description);
        b.setCategoryFk(category);
        b.setUserFk(owner);
        b.setCreatedDate(LocalDateTime.now());
        b.setLastUpdate(LocalDateTime.now());
        return businessRepository.save(b);
    }

    private void location(BusinessEntity business, CityEntity city, boolean main) {
        var l = new LocationEntity();
        l.setBusiness(business);
        l.setCity(city);
        l.setAddressLine("1 Main St");
        l.setMain(main);
        l.setCreatedDate(LocalDateTime.now());
        l.setLastUpdate(LocalDateTime.now());
        locationRepository.save(l);
    }

    private List<String> names(BusinessSearchCriteria criteria) {
        return businessRepository.findAll(BusinessSpecifications.from(criteria),
                PageRequest.of(0, 20, Sort.by("name"))).map(BusinessEntity::getName).getContent();
    }

    @Test
    void noFilterReturnsEveryBusiness() {
        assertEquals(4, names(new BusinessSearchCriteria(null, null, null, null)).size());
    }

    @Test
    void byCityIdReturnsTheBusinessesWithALocationThere() {
        assertEquals(List.of("Panadería La Estrella", "Taquería El Sol"), names(new BusinessSearchCriteria(toronto.getId(), null, null, null)));
    }

    @Test
    void aBusinessWithTwoLocationsInTheCityAppearsOnce() {
        var result = names(new BusinessSearchCriteria(ottawa.getId(), null, null, null));
        assertEquals(List.of("Abogado Pérez", "Panadería La Estrella"), result);
    }

    @Test
    void byCityNameIsCaseInsensitive() {
        assertEquals(List.of("Abogado Pérez", "Panadería La Estrella"), names(new BusinessSearchCriteria(null, "  OTTAWA ", null, null)));
    }

    @Test
    void cityIdWinsOverCityName() {
        assertEquals(List.of("Panadería La Estrella", "Taquería El Sol"), names(new BusinessSearchCriteria(toronto.getId(), "Ottawa", null, null)));
    }

    @Test
    void aCategoryIncludesItsSubCategories() {
        // Food has the bakery as a child: the panadería (bakery) and the taquería (food) both match
        assertEquals(List.of("Panadería La Estrella", "Taquería El Sol"), names(new BusinessSearchCriteria(null, null, food.getId(), null)));
        assertEquals(List.of("Panadería La Estrella"), names(new BusinessSearchCriteria(null, null, bakery.getId(), null)));
    }

    @Test
    void aTopLevelCategoryWithoutChildrenStillMatchesItsOwnBusinesses() {
        assertEquals(List.of("Abogado Pérez", "Sin sede"), names(new BusinessSearchCriteria(null, null, services.getId(), null)));
    }

    @Test
    void textMatchesNameOrDescriptionIgnoringCase() {
        assertEquals(List.of("Taquería El Sol"), names(new BusinessSearchCriteria(null, null, null, "TACOS")));
        assertEquals(List.of("Abogado Pérez"), names(new BusinessSearchCriteria(null, null, null, "abogado")));
    }

    @Test
    void likeWildcardsInTheTextAreLiterals() {
        assertEquals(List.of("Panadería La Estrella"), names(new BusinessSearchCriteria(null, null, null, "100%")));
        assertEquals(List.of("Panadería La Estrella"), names(new BusinessSearchCriteria(null, null, null, "casero_fresco")));
        assertTrue(names(new BusinessSearchCriteria(null, null, null, "casero?fresco")).isEmpty());
        assertTrue(names(new BusinessSearchCriteria(null, null, null, "%")).size() <= 1);
    }

    @Test
    void filtersAreCombinedWithAnd() {
        assertEquals(List.of("Taquería El Sol"), names(new BusinessSearchCriteria(toronto.getId(), null, food.getId(), "taco")));
        assertTrue(names(new BusinessSearchCriteria(ottawa.getId(), null, food.getId(), "taco")).isEmpty());
    }

    @Test
    void locationQueriesFetchTheCityAndOrderTheMainFirst() {
        var list = locationRepository.findByBusinessId(panaderia.getId());
        assertEquals(2, list.size());
        assertTrue(list.get(0).isMain());
        assertEquals("Toronto", list.get(0).getCity().getName());
        assertEquals(2, locationRepository.countByBusinessId(panaderia.getId()));
        assertEquals(toronto.getId(), locationRepository.findMainByBusinessId(panaderia.getId()).orElseThrow().getCity().getId());
        assertEquals(3, locationRepository.findByBusinessIdIn(List.of(panaderia.getId(), taqueria.getId())).size());
        assertTrue(locationRepository.findByIdAndBusinessId(list.get(0).getId(), taqueria.getId()).isEmpty());
        assertTrue(locationRepository.findByBusinessId(sinSede.getId()).isEmpty());
    }

    @Test
    void theCitiesListingFiltersByCountryAndText() {
        assertEquals(2, cityRepository.findAll(CitySpecifications.from(null, null)).size());
        assertEquals(2, cityRepository.findAll(CitySpecifications.from("ca", "")).size());
        assertEquals(0, cityRepository.findAll(CitySpecifications.from("US", null)).size());
        assertEquals(List.of("Ottawa"), cityRepository.findAll(CitySpecifications.from(null, "TTA")).stream().map(CityEntity::getName).toList());
    }
}
