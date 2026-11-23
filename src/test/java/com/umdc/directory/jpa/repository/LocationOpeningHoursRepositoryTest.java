package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.entity.LocationOpeningHoursEntity;
import com.umdc.directory.jpa.entity.TimezoneEntity;
import com.umdc.directory.jpa.entity.UserEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Runs the real queries of the opening hours against H2 in PostgreSQL mode.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:hoursdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=USER;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS directory_site\\\\;CREATE SCHEMA IF NOT EXISTS general\\\\;"
                + "CREATE ALIAS IF NOT EXISTS directory_site.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"\\\\;"
                + "CREATE ALIAS IF NOT EXISTS general.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=test"
})
class LocationOpeningHoursRepositoryTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private LocationOpeningHoursRepository repository;
    @Autowired
    private BusinessRepository businessRepository;

    private LocationOpeningHoursEntity slot(UUID location, int day, String from, String to) {
        return new LocationOpeningHoursEntity(location, day, LocalTime.parse(from), LocalTime.parse(to), false);
    }

    @Test
    void slotsComeBackInWeekOrderAndOnlyForTheLocation() {
        var location = UUID.randomUUID();
        repository.save(slot(location, 3, "09:00", "12:00"));
        repository.save(slot(location, 1, "15:00", "19:00"));
        repository.save(slot(location, 1, "09:00", "13:00"));
        repository.save(new LocationOpeningHoursEntity(location, 7, null, null, true));
        repository.save(slot(UUID.randomUUID(), 1, "08:00", "09:00"));
        em.flush();
        em.clear();

        var slots = repository.findByLocationId(location);
        assertEquals(4, slots.size());
        assertEquals(LocalTime.of(9, 0), slots.get(0).getOpensAt());
        assertEquals(LocalTime.of(15, 0), slots.get(1).getOpensAt());
        assertEquals(3, slots.get(2).getDayOfWeek());
        assertTrue(slots.get(3).isClosed());
    }

    @Test
    void deleteRemovesOnlyTheSlotsOfTheLocation() {
        var location = UUID.randomUUID();
        var other = UUID.randomUUID();
        repository.save(slot(location, 1, "09:00", "13:00"));
        repository.save(slot(location, 2, "09:00", "13:00"));
        repository.save(slot(other, 1, "09:00", "13:00"));

        assertEquals(2, repository.deleteByLocationId(location));
        assertTrue(repository.findByLocationId(location).isEmpty());
        assertEquals(1, repository.findByLocationId(other).size());
    }

    @Test
    void theTimezoneOfABusinessIsFoundThroughItsForeignKey() {
        var timezone = new TimezoneEntity();
        timezone.setId(UUID.randomUUID());
        timezone.setName("America/Toronto");
        timezone.setAbbreviation("EST");
        timezone.setUtcOffset(Duration.ofHours(-5));
        timezone.setCreatedAt(LocalDateTime.now());
        timezone.setLastUpdated(LocalDateTime.now());
        em.persist(timezone);
        var owner = new UserEntity();
        em.persist(owner);
        var category = new CategoryEntity();
        category.setName("Food");
        category.setDescription("Food");
        category.setActive(true);
        category.setCreatedDate(LocalDateTime.now());
        category.setLastUpdate(LocalDateTime.now());
        em.persist(category);
        var withZone = business("A", category, owner, timezone.getId());
        var withoutZone = business("B", category, owner, null);
        em.flush();
        em.clear();

        assertEquals("America/Toronto", repository.findTimezoneOfBusiness(withZone.getId()).orElseThrow().getName());
        assertTrue(repository.findTimezoneOfBusiness(withoutZone.getId()).isEmpty());
        assertTrue(repository.findTimezoneOfBusiness(UUID.randomUUID()).isEmpty());
    }

    private BusinessEntity business(String name, CategoryEntity category, UserEntity owner, UUID timezoneId) {
        var b = new BusinessEntity();
        b.setName(name);
        b.setCategoryFk(category);
        b.setUserFk(owner);
        b.setTimezoneFk(timezoneId);
        b.setCreatedDate(LocalDateTime.now());
        b.setLastUpdate(LocalDateTime.now());
        return businessRepository.save(b);
    }
}
