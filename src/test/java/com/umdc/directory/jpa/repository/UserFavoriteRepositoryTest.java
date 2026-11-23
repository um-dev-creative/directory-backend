package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CampaignEntity;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.entity.ProductEntity;
import com.umdc.directory.jpa.entity.UserEntity;
import com.umdc.directory.jpa.entity.UserFavoriteEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Runs the real paginated favorites query against H2 in PostgreSQL mode.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:favdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=USER;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS directory_site\\\\;CREATE SCHEMA IF NOT EXISTS general\\\\;"
                + "CREATE ALIAS IF NOT EXISTS directory_site.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"\\\\;"
                + "CREATE ALIAS IF NOT EXISTS general.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=test"
})
class UserFavoriteRepositoryTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private UserFavoriteRepository repository;

    private UserEntity user;
    private UserEntity other;

    @BeforeEach
    void data() {
        user = new UserEntity();
        other = new UserEntity();
        em.persist(user);
        em.persist(other);
        var category = new CategoryEntity();
        category.setName("Food");
        category.setDescription("Food");
        category.setActive(true);
        category.setCreatedDate(LocalDateTime.now());
        category.setLastUpdate(LocalDateTime.now());
        em.persist(category);

        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 10, 0);
        BusinessEntity zeta = business("Zeta", category);
        BusinessEntity alpha = business("Alpha", category);
        ProductEntity mango = product("Mango", category);
        CampaignEntity beta = campaign("Beta", category, alpha);
        favorite(user, zeta, null, null, base, true);
        favorite(user, alpha, null, null, base.plusDays(1), true);
        favorite(user, null, mango, null, base.plusDays(2), true);
        favorite(user, null, null, beta, base.plusDays(3), true);
        favorite(user, business("Inactive", category), null, null, base.plusDays(4), false);
        favorite(other, business("Foreign", category), null, null, base.plusDays(5), true);
        em.flush();
        em.clear();
    }

    private BusinessEntity business(String name, CategoryEntity category) {
        var b = new BusinessEntity();
        b.setName(name);
        b.setCategoryFk(category);
        b.setUserFk(user);
        b.setCreatedDate(LocalDateTime.now());
        b.setLastUpdate(LocalDateTime.now());
        em.persist(b);
        return b;
    }

    private ProductEntity product(String name, CategoryEntity category) {
        var p = new ProductEntity();
        p.setName(name);
        p.setDescription(name);
        p.setCategoryFk(category);
        p.setCreatedDate(LocalDateTime.now());
        p.setLastUpdate(LocalDateTime.now());
        p.setActive(true);
        em.persist(p);
        return p;
    }

    private CampaignEntity campaign(String title, CategoryEntity category, BusinessEntity business) {
        var c = new CampaignEntity();
        c.setTitle(title);
        c.setStartDate(LocalDateTime.now());
        c.setEndDate(LocalDateTime.now().plusDays(1));
        c.setCategoryFk(category);
        c.setBusinessFk(business);
        c.setCreatedDate(LocalDateTime.now());
        c.setLastUpdate(LocalDateTime.now());
        em.persist(c);
        return c;
    }

    private void favorite(UserEntity owner, BusinessEntity b, ProductEntity p, CampaignEntity c,
                          LocalDateTime createdAt, boolean active) {
        var f = new UserFavoriteEntity(owner);
        f.setBusiness(b);
        f.setProduct(p);
        f.setCampaign(c);
        f.setActive(active);
        f.setCreatedAt(createdAt);
        f.setUpdatedAt(createdAt);
        em.persist(f);
    }

    private static List<String> names(Page<UserFavoriteEntity> page) {
        return page.getContent().stream().map(f -> f.getBusiness() != null ? f.getBusiness().getName()
                : f.getProduct() != null ? f.getProduct().getName() : f.getCampaign().getTitle()).toList();
    }

    @Test
    void allTypesAreOrderedByBucketThenCreatedAtDescAndOnlyActiveOfTheUser() {
        Page<UserFavoriteEntity> page = repository.findPageByUser(user.getId(), -1,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertEquals(4, page.getTotalElements());
        assertEquals(List.of("Alpha", "Zeta", "Mango", "Beta"), names(page));
    }

    @Test
    void sortByNameAscUsesTheNameOfEachTarget() {
        Page<UserFavoriteEntity> stores = repository.findPageByUser(user.getId(), 0,
                PageRequest.of(0, 10, JpaSort.unsafe(Sort.Direction.ASC, "COALESCE(b.name, p.name, c.title)")));

        assertEquals(List.of("Alpha", "Zeta"), names(stores));
    }

    @Test
    void paginationIsDoneByTheDatabaseAndKeepsTheTotal() {
        PageRequest first = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserFavoriteEntity> p0 = repository.findPageByUser(user.getId(), -1, first);
        Page<UserFavoriteEntity> p1 = repository.findPageByUser(user.getId(), -1, first.next());

        assertEquals(List.of("Alpha", "Zeta", "Mango"), names(p0));
        assertEquals(List.of("Beta"), names(p1));
        assertEquals(4, p1.getTotalElements());
    }

    @Test
    void typeFiltersReturnOnlyThatBucket() {
        PageRequest any = PageRequest.of(0, 10, Sort.by("createdAt"));
        assertEquals(List.of("Mango"), names(repository.findPageByUser(user.getId(), 1, any)));
        assertEquals(List.of("Beta"), names(repository.findPageByUser(user.getId(), 2, any)));
        assertTrue(repository.findPageByUser(UUID.randomUUID(), -1, any).isEmpty());
    }
}
