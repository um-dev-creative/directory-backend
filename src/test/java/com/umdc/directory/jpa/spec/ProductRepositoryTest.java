package com.umdc.directory.jpa.spec;

import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.BusinessProductEntity;
import com.umdc.directory.jpa.entity.BusinessProductEntityId;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.entity.ProductEntity;
import com.umdc.directory.jpa.entity.UserEntity;
import com.umdc.directory.jpa.repository.BusinessProductRepository;
import com.umdc.directory.jpa.repository.BusinessRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The product queries of RF-02 against H2: the join on the product status and the ownership count.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:productdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=USER;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS directory_site\\\\;CREATE SCHEMA IF NOT EXISTS general\\\\;"
                + "CREATE ALIAS IF NOT EXISTS directory_site.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"\\\\;"
                + "CREATE ALIAS IF NOT EXISTS general.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=test"
})
@DisplayName("Product repository queries")
class ProductRepositoryTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private BusinessRepository businessRepository;
    @Autowired
    private BusinessProductRepository repository;

    private UserEntity owner;
    private UserEntity stranger;
    private BusinessEntity business;
    private ProductEntity active;
    private ProductEntity inactive;
    private ProductEntity linkInactive;

    @BeforeEach
    void data() {
        owner = new UserEntity();
        em.persist(owner);
        stranger = new UserEntity();
        em.persist(stranger);
        var category = new CategoryEntity();
        category.setName("Food");
        category.setDescription("Food");
        category.setActive(true);
        category.setCreatedDate(LocalDateTime.now());
        category.setLastUpdate(LocalDateTime.now());
        em.persist(category);

        business = new BusinessEntity();
        business.setName("Shop");
        business.setCategoryFk(category);
        business.setUserFk(owner);
        business.setCreatedDate(LocalDateTime.now());
        business.setLastUpdate(LocalDateTime.now());
        business = businessRepository.save(business);

        active = product(category, "active", true);
        active.setPrice(new BigDecimal("12.34"));
        active.setCurrency("USD");
        active.setImageRef("img");
        inactive = product(category, "inactive", true);
        inactive.setActive(false);
        linkInactive = product(category, "link inactive", true);
        link(active, true);
        link(inactive, true);
        link(linkInactive, false);
        em.flush();
        em.clear();
    }

    private ProductEntity product(CategoryEntity category, String name, boolean isActive) {
        var p = new ProductEntity();
        p.setName(name);
        p.setDescription(name);
        p.setCategoryFk(category);
        p.setActive(isActive);
        p.setCreatedDate(LocalDateTime.now());
        p.setLastUpdate(LocalDateTime.now());
        em.persist(p);
        return p;
    }

    private void link(ProductEntity p, boolean isActive) {
        var l = new BusinessProductEntity();
        l.setId(new BusinessProductEntityId(business.getId(), p.getId()));
        l.setBusiness(business);
        l.setProduct(p);
        l.setActive(isActive);
        l.setCreatedDate(LocalDateTime.now());
        l.setLastUpdate(LocalDateTime.now());
        em.persist(l);
    }

    @Test
    @DisplayName("public query returns only active links of active products, with price and image persisted")
    void publicQuery() {
        var page = repository.findByBusinessAndActiveAndProductActive(business, true, true, PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
        var p = page.getContent().getFirst().getProduct();
        assertEquals("active", p.getName());
        assertEquals(new BigDecimal("12.34"), p.getPrice());
        assertEquals("USD", p.getCurrency());
        assertEquals("img", p.getImageRef());
    }

    @Test
    @DisplayName("owner queries: all links, or filtered by the product status")
    void ownerQueries() {
        assertEquals(3, repository.findByBusiness(business, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, repository.findByBusinessAndProductActive(business, false, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(2, repository.findByBusinessAndProductActive(business, true, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    @DisplayName("countOwnedLinks counts only the businesses of the user")
    void ownership() {
        assertEquals(1, repository.countOwnedLinks(active.getId(), owner.getId()));
        assertEquals(0, repository.countOwnedLinks(active.getId(), stranger.getId()));
    }
}
