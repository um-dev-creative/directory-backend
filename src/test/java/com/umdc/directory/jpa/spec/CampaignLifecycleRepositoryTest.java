package com.umdc.directory.jpa.spec;

import java.util.UUID;
import com.umdc.directory.api.v1.service.CampaignStatusCounter;
import com.umdc.directory.constant.CampaignStatus;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CampaignEntity;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.repository.CampaignRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// Runs the status, type and visibility specifications and the status counter against H2.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CampaignStatusCounter.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:campaigndb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=USER;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS directory_site\\\\;CREATE SCHEMA IF NOT EXISTS general\\\\;"
                + "CREATE ALIAS IF NOT EXISTS directory_site.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"\\\\;"
                + "CREATE ALIAS IF NOT EXISTS general.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=test"
})
class CampaignLifecycleRepositoryTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private CampaignRepository repository;
    @Autowired
    private CampaignStatusCounter counter;

    private final LocalDateTime now = LocalDateTime.now();
    private UUID owner;
    private UUID other;
    private BusinessEntity ownBusiness;
    private BusinessEntity otherBusiness;
    private CategoryEntity category;

    @BeforeEach
    void data() {
        owner = UUID.randomUUID();
        other = UUID.randomUUID();
        category = new CategoryEntity();
        category.setName("c");
        category.setDescription("c");
        category.setActive(true);
        category.setCreatedDate(now);
        category.setLastUpdate(now);
        em.persist(category);
        ownBusiness = business(owner);
        otherBusiness = business(other);

        campaign("active", ownBusiness, true, null, "PROMOTION", -2, 5);
        campaign("scheduled", ownBusiness, true, null, "EVENT", 2, 9);
        campaign("paused", ownBusiness, false, null, "PROMOTION", -2, 5);
        campaign("expired", ownBusiness, true, null, "ANNOUNCEMENT", -9, -2);
        campaign("draft", ownBusiness, true, "DRAFT", "PROMOTION", -1, 5);
        campaign("other-active", otherBusiness, true, null, "EVENT", -1, 5);
        campaign("other-paused", otherBusiness, false, null, "EVENT", -1, 5);
        em.flush();
        em.clear();
    }

    private BusinessEntity business(UUID user) {
        var b = new BusinessEntity();
        b.setName("b");
        b.setCategoryFk(category);
        b.setUserFk(user);
        b.setCreatedDate(now);
        b.setLastUpdate(now);
        em.persist(b);
        return b;
    }

    private void campaign(String title, BusinessEntity business, boolean active, String stored, String type, int startDays, int endDays) {
        var c = new CampaignEntity();
        c.setTitle(title);
        c.setBusinessFk(business);
        c.setCategoryFk(category);
        c.setActive(active);
        c.setStatus(stored);
        c.setType(type);
        c.setStartDate(now.plusDays(startDays));
        c.setEndDate(now.plusDays(endDays));
        c.setCreatedDate(now);
        c.setLastUpdate(now);
        em.persist(c);
    }

    private Set<String> titles(Specification<CampaignEntity> spec) {
        return repository.findAll(spec).stream().map(CampaignEntity::getTitle).collect(Collectors.toSet());
    }

    @Test
    void eachStatusMatchesItsOwnCampaignsOnly() {
        assertEquals(Set.of("active", "other-active"), titles(CampaignSpecifications.byStatus(CampaignStatus.ACTIVE, now)));
        assertEquals(Set.of("scheduled"), titles(CampaignSpecifications.byStatus(CampaignStatus.SCHEDULED, now)));
        assertEquals(Set.of("paused", "other-paused"), titles(CampaignSpecifications.byStatus(CampaignStatus.PAUSED, now)));
        assertEquals(Set.of("expired"), titles(CampaignSpecifications.byStatus(CampaignStatus.EXPIRED, now)));
        assertEquals(Set.of("draft"), titles(CampaignSpecifications.byStatus(CampaignStatus.DRAFT, now)));
    }

    @Test
    void publicSeesScheduledAndActiveOnly() {
        assertEquals(Set.of("active", "scheduled", "other-active"), titles(CampaignSpecifications.visibleTo(null, now)));
    }

    @Test
    void ownerSeesEverythingOfItsBusinessPlusPublicOnes() {
        assertEquals(Set.of("active", "scheduled", "paused", "expired", "draft", "other-active"),
                titles(CampaignSpecifications.visibleTo(owner, now)));
    }

    @Test
    void ownerFilteredByBusinessAndStatusIsHiddenFromOthers() {
        Specification<CampaignEntity> mine = CampaignSpecifications.byFilters(null, null, ownBusiness.getId(), null)
                .and(CampaignSpecifications.byStatus(CampaignStatus.PAUSED, now));
        assertEquals(Set.of("paused"), titles(mine.and(CampaignSpecifications.visibleTo(owner, now))));
        assertEquals(Set.of(), titles(mine.and(CampaignSpecifications.visibleTo(other, now))));
        assertEquals(Set.of(), titles(mine.and(CampaignSpecifications.visibleTo(null, now))));
    }

    @Test
    void typeFilterIsCaseInsensitive() {
        assertEquals(Set.of("scheduled", "other-active", "other-paused"), titles(CampaignSpecifications.byType("event")));
    }

    @Test
    void counterUsesTheActiveFlagAndDates() {
        // all campaigns: active flag true and not expired = active, scheduled, draft, other-active; inactive = 2; expired = 1
        assertArrayEquals(new long[]{4, 2, 1}, counter.countByStatus(Specification.unrestricted(), now));
        List<CampaignEntity> all = repository.findAll();
        assertEquals(7, all.size());
    }
}
