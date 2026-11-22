package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.BusinessViewDailyEntity;
import com.umdc.directory.jpa.entity.BusinessViewDailyId;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.entity.UserEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Runs the real queries of the view counters against H2 in PostgreSQL mode.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:viewsdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=USER;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS directory_site\\\\;CREATE SCHEMA IF NOT EXISTS general\\\\;"
                + "CREATE ALIAS IF NOT EXISTS directory_site.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"\\\\;"
                + "CREATE ALIAS IF NOT EXISTS general.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=test"
})
class BusinessViewDailyRepositoryTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 21);

    @Autowired
    private EntityManager em;
    @Autowired
    private BusinessViewDailyRepository viewRepository;
    @Autowired
    private BusinessRepository businessRepository;

    private BusinessEntity business;
    private BusinessEntity other;
    private UserEntity owner;

    @BeforeEach
    void data() {
        owner = new UserEntity();
        em.persist(owner);
        var category = new CategoryEntity();
        category.setName("Food");
        category.setDescription("Food");
        category.setActive(true);
        category.setCreatedDate(LocalDateTime.now());
        category.setLastUpdate(LocalDateTime.now());
        em.persist(category);
        business = business("A", category);
        other = business("B", category);
        em.flush();
        em.clear();
    }

    private BusinessEntity business(String name, CategoryEntity category) {
        var b = new BusinessEntity();
        b.setName(name);
        b.setCategoryFk(category);
        b.setUserFk(owner);
        b.setCreatedDate(LocalDateTime.now());
        b.setLastUpdate(LocalDateTime.now());
        return businessRepository.save(b);
    }

    @Test
    void incrementUpdatesNothingWhenTheDayHasNoRow() {
        assertEquals(0, viewRepository.increment(business.getId(), DAY));
    }

    @Test
    void incrementAddsOneToTheExistingRowOnly() {
        viewRepository.saveAndFlush(new BusinessViewDailyEntity(new BusinessViewDailyId(business.getId(), DAY), 1));
        viewRepository.saveAndFlush(new BusinessViewDailyEntity(new BusinessViewDailyId(other.getId(), DAY), 10));

        assertEquals(1, viewRepository.increment(business.getId(), DAY));
        assertEquals(1, viewRepository.increment(business.getId(), DAY));
        em.clear();

        assertEquals(3, viewRepository.findById(new BusinessViewDailyId(business.getId(), DAY)).orElseThrow().getViews());
        assertEquals(10, viewRepository.findById(new BusinessViewDailyId(other.getId(), DAY)).orElseThrow().getViews());
        assertEquals(0, viewRepository.increment(business.getId(), DAY.plusDays(1)));
    }

    @Test
    void seriesIsOrderedFilteredByBusinessAndBothEndsAreIncluded() {
        for (int offset : new int[]{-10, -3, 0, 2, 5}) {
            viewRepository.save(new BusinessViewDailyEntity(new BusinessViewDailyId(business.getId(), DAY.plusDays(offset)), offset + 20L));
        }
        viewRepository.save(new BusinessViewDailyEntity(new BusinessViewDailyId(other.getId(), DAY), 99));
        viewRepository.flush();
        em.clear();

        List<BusinessViewDailyEntity> series = viewRepository.findSeries(business.getId(), DAY.minusDays(3), DAY.plusDays(2));

        assertEquals(List.of(DAY.minusDays(3), DAY, DAY.plusDays(2)), series.stream().map(r -> r.getId().getViewDate()).toList());
        assertTrue(series.stream().allMatch(r -> r.getId().getBusinessId().equals(business.getId())));
        assertTrue(viewRepository.findSeries(business.getId(), DAY.plusDays(10), DAY.plusDays(20)).isEmpty());
    }

    @Test
    void theOwnerIdIsReadWithoutLoadingTheBusiness() {
        assertEquals(owner.getId(), businessRepository.findOwnerId(business.getId()).orElseThrow());
        assertTrue(businessRepository.findOwnerId(UUID.randomUUID()).isEmpty());
    }

    @Test
    void theCompositeKeyComparesByBusinessAndDay() {
        var id = new BusinessViewDailyId(business.getId(), DAY);
        assertEquals(id, new BusinessViewDailyId(business.getId(), DAY));
        assertEquals(id.hashCode(), new BusinessViewDailyId(business.getId(), DAY).hashCode());
        assertNotEquals(id, new BusinessViewDailyId(business.getId(), DAY.plusDays(1)));
        assertNotEquals(null, id);
        assertNotEquals("x", id);
    }
}
