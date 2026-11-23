package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.BusinessEventDailyEntity;
import com.umdc.directory.jpa.entity.CampaignEntity;
import com.umdc.directory.jpa.entity.BusinessEventDailyId;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Runs the real queries of the event counters against H2 in PostgreSQL mode.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:eventsdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=USER;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS directory_site\\\\;CREATE SCHEMA IF NOT EXISTS general\\\\;"
                + "CREATE ALIAS IF NOT EXISTS directory_site.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"\\\\;"
                + "CREATE ALIAS IF NOT EXISTS general.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=test"
})
class BusinessEventDailyRepositoryTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 21);

    @Autowired
    private EntityManager em;
    @Autowired
    private BusinessEventDailyRepository eventRepository;
    @Autowired
    private BusinessRepository businessRepository;
    @Autowired
    private CampaignRepository campaignRepository;

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

    private static final UUID NIL = BusinessEventDailyId.NO_CAMPAIGN;

    private void save(UUID businessId, UUID campaign, String type, LocalDate day, long count) {
        eventRepository.saveAndFlush(new BusinessEventDailyEntity(new BusinessEventDailyId(businessId, campaign, type, day), count));
    }

    private long count(UUID businessId, UUID campaign, String type, LocalDate day) {
        return eventRepository.findById(new BusinessEventDailyId(businessId, campaign, type, day)).orElseThrow().getEventCount();
    }

    @Test
    void incrementUpdatesNothingWhenTheKeyHasNoRow() {
        assertEquals(0, eventRepository.increment(business.getId(), NIL, "CALL", DAY));
    }

    @Test
    void incrementAddsOneToTheExactKeyOnly() {
        UUID campaign = UUID.randomUUID();
        save(business.getId(), NIL, "CALL", DAY, 1);
        save(business.getId(), campaign, "CALL", DAY, 5);
        save(business.getId(), NIL, "WEB", DAY, 7);
        save(other.getId(), NIL, "CALL", DAY, 10);

        assertEquals(1, eventRepository.increment(business.getId(), NIL, "CALL", DAY));
        assertEquals(1, eventRepository.increment(business.getId(), NIL, "CALL", DAY));
        em.clear();

        assertEquals(3, count(business.getId(), NIL, "CALL", DAY));
        assertEquals(5, count(business.getId(), campaign, "CALL", DAY));
        assertEquals(7, count(business.getId(), NIL, "WEB", DAY));
        assertEquals(10, count(other.getId(), NIL, "CALL", DAY));
        assertEquals(0, eventRepository.increment(business.getId(), NIL, "CALL", DAY.plusDays(1)));
    }

    /// The increment is arithmetic done by the database (`count = count + 1`), not read-modify-write in Java: a stale copy of the
    /// row held by another session (as a concurrent request would hold) does not make it lose updates.
    @Test
    void incrementIsDoneByTheDatabaseSoAStaleCopyDoesNotLoseUpdates() {
        save(business.getId(), NIL, "ROUTE", DAY, 10);
        em.clear();
        var stale = eventRepository.findById(new BusinessEventDailyId(business.getId(), NIL, "ROUTE", DAY)).orElseThrow();
        assertEquals(10, stale.getEventCount());

        for (int i = 0; i < 100; i++) {
            assertEquals(1, eventRepository.increment(business.getId(), NIL, "ROUTE", DAY));
        }

        assertEquals(10, stale.getEventCount());
        em.clear();
        assertEquals(110, count(business.getId(), NIL, "ROUTE", DAY));
    }

    @Test
    void seriesIsOrderedFilteredByBusinessAndBothEndsAreIncluded() {
        UUID campaign = new UUID(0L, 1L);
        save(business.getId(), NIL, "CALL", DAY.minusDays(10), 1);
        save(business.getId(), campaign, "WEB", DAY, 2);
        save(business.getId(), NIL, "WEB", DAY, 3);
        save(business.getId(), NIL, "CALL", DAY, 4);
        save(business.getId(), NIL, "CALL", DAY.minusDays(3), 5);
        save(business.getId(), NIL, "CALL", DAY.plusDays(5), 6);
        save(other.getId(), NIL, "CALL", DAY, 99);
        em.clear();

        var series = eventRepository.findSeries(business.getId(), DAY.minusDays(3), DAY.plusDays(2));

        assertEquals(List.of(5L, 4L, 3L, 2L), series.stream().map(BusinessEventDailyEntity::getEventCount).toList());
        assertTrue(series.stream().allMatch(r -> r.getId().getBusinessId().equals(business.getId())));
        assertTrue(eventRepository.findSeries(business.getId(), DAY.plusDays(10), DAY.plusDays(20)).isEmpty());
    }

    @Test
    void theCompositeKeyComparesByEveryPart() {
        var id = new BusinessEventDailyId(business.getId(), NIL, "CALL", DAY);
        assertEquals(id, new BusinessEventDailyId(business.getId(), NIL, "CALL", DAY));
        assertEquals(id.hashCode(), new BusinessEventDailyId(business.getId(), NIL, "CALL", DAY).hashCode());
        assertNotEquals(id, new BusinessEventDailyId(business.getId(), NIL, "CALL", DAY.plusDays(1)));
        assertNotEquals(id, new BusinessEventDailyId(business.getId(), NIL, "WEB", DAY));
        assertNotEquals(id, new BusinessEventDailyId(business.getId(), new UUID(0L, 1L), "CALL", DAY));
        assertNotEquals(id, new BusinessEventDailyId(other.getId(), NIL, "CALL", DAY));
        assertNotEquals(null, id);
        assertNotEquals("x", id);
        assertEquals(id, id);
    }

    @Test
    void theEntityAndKeyAccessorsWork() {
        var id = new BusinessEventDailyId();
        id.setBusinessId(business.getId());
        id.setCampaignId(NIL);
        id.setEventType("SAVE");
        id.setEventDate(DAY);
        var entity = new BusinessEventDailyEntity();
        entity.setId(id);
        entity.setEventCount(2);
        assertEquals(2, entity.getEventCount());
        assertEquals("SAVE", entity.getId().getEventType());
        assertEquals(NIL, entity.getId().getCampaignId());
        assertEquals(DAY, entity.getId().getEventDate());
        assertEquals(business.getId(), entity.getId().getBusinessId());
    }

    @Test
    void campaignOwnershipQueryMatchesTheBusinessOfTheCampaign() {
        var category = em.createQuery("SELECT c FROM CategoryEntity c", CategoryEntity.class).getResultList().getFirst();
        var campaign = new CampaignEntity();
        campaign.setTitle("C");
        campaign.setBusinessFk(business);
        campaign.setCategoryFk(category);
        campaign.setActive(true);
        campaign.setStartDate(LocalDateTime.now());
        campaign.setEndDate(LocalDateTime.now().plusDays(1));
        campaign.setCreatedDate(LocalDateTime.now());
        campaign.setLastUpdate(LocalDateTime.now());
        em.persist(campaign);
        em.flush();
        em.clear();
        assertTrue(campaignRepository.existsByIdAndBusinessFk_Id(campaign.getId(), business.getId()));
        assertFalse(campaignRepository.existsByIdAndBusinessFk_Id(campaign.getId(), other.getId()));
        assertFalse(campaignRepository.existsByIdAndBusinessFk_Id(UUID.randomUUID(), business.getId()));
    }
}
