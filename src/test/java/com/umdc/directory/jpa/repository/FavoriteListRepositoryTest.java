package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.entity.FavoriteListEntity;
import com.umdc.directory.jpa.entity.FavoriteListItemEntity;
import com.umdc.directory.jpa.entity.UserFavoriteEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Runs the queries of the favorite lists against H2 in PostgreSQL mode.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:favlistdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=USER;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS directory_site\\\\;CREATE SCHEMA IF NOT EXISTS general\\\\;"
                + "CREATE ALIAS IF NOT EXISTS directory_site.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"\\\\;"
                + "CREATE ALIAS IF NOT EXISTS general.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=test"
})
class FavoriteListRepositoryTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private FavoriteListRepository lists;
    @Autowired
    private FavoriteListItemRepository items;
    @Autowired
    private UserFavoriteRepository favorites;
    @Autowired
    private BusinessRepository businesses;

    private CategoryEntity category() {
        var category = new CategoryEntity();
        category.setName("Food");
        category.setDescription("Food");
        category.setActive(true);
        category.setCreatedDate(LocalDateTime.now());
        category.setLastUpdate(LocalDateTime.now());
        em.persist(category);
        return category;
    }

    private UserFavoriteEntity favorite(UUID user, String businessName, boolean active, CategoryEntity category) {
        var b = new BusinessEntity();
        b.setName(businessName);
        b.setCategoryFk(category);
        b.setUserFk(UUID.randomUUID());
        b.setCreatedDate(LocalDateTime.now());
        b.setLastUpdate(LocalDateTime.now());
        var fav = new UserFavoriteEntity(user);
        fav.setBusiness(businesses.save(b));
        fav.setActive(active);
        fav.setCreatedAt(LocalDateTime.now());
        fav.setUpdatedAt(LocalDateTime.now());
        return favorites.saveAndFlush(fav);
    }

    private FavoriteListEntity list(UUID user, String name, LocalDateTime created) {
        var l = new FavoriteListEntity();
        l.setUserFk(user);
        l.setName(name);
        l.setCreatedAt(created);
        l.setUpdatedAt(created);
        return lists.saveAndFlush(l);
    }

    private FavoriteListItemEntity item(FavoriteListEntity list, UserFavoriteEntity fav, LocalDateTime at) {
        var i = new FavoriteListItemEntity();
        i.setListFk(list.getId());
        i.setFavorite(fav);
        i.setCreatedAt(at);
        return items.saveAndFlush(i);
    }

    @Test
    void listQueriesAreScopedToTheOwner() {
        var me = UUID.randomUUID();
        var other = UUID.randomUUID();
        var now = LocalDateTime.now();
        var older = list(me, "Pizza", now.minusDays(1));
        var newer = list(me, "Sushi", now);
        var foreign = list(other, "Pizza", now);

        var found = lists.findByUserFkOrderByCreatedAtDesc(me);
        assertEquals(2, found.size());
        assertEquals(newer.getId(), found.get(0).getId());
        assertEquals(2, lists.countByUserFk(me));
        assertTrue(lists.findByIdAndUserFk(older.getId(), me).isPresent());
        assertFalse(lists.findByIdAndUserFk(foreign.getId(), me).isPresent());
        assertTrue(lists.existsByUserFkAndNameIgnoreCase(me, "pizza"));
        assertFalse(lists.existsByUserFkAndNameIgnoreCase(me, "tacos"));
        assertFalse(lists.existsByUserFkAndNameIgnoreCaseAndIdNot(me, "pizza", older.getId()));
        assertTrue(lists.existsByUserFkAndNameIgnoreCaseAndIdNot(me, "pizza", newer.getId()));
    }

    @Test
    void itemsAreCountedPagedAndDeletedByList() {
        var me = UUID.randomUUID();
        var category = category();
        var now = LocalDateTime.now();
        var a = list(me, "A", now);
        var b = list(me, "B", now);
        var empty = list(me, "Empty", now);
        var f1 = favorite(me, "One", true, category);
        var f2 = favorite(me, "Two", true, category);
        var gone = favorite(me, "Gone", false, category);
        item(a, f1, now.minusMinutes(2));
        item(a, f2, now.minusMinutes(1));
        item(a, gone, now);
        item(b, f1, now);
        em.clear();

        var counts = items.countActiveByLists(List.of(a.getId(), b.getId(), empty.getId()));
        assertEquals(2, counts.size());
        assertEquals(2L, counts.stream().filter(r -> r[0].equals(a.getId())).findFirst().orElseThrow()[1]);
        assertEquals(1L, counts.stream().filter(r -> r[0].equals(b.getId())).findFirst().orElseThrow()[1]);
        assertEquals(2, items.countActiveByList(a.getId()));
        assertEquals(0, items.countActiveByList(empty.getId()));

        var page = items.findPageByList(a.getId(), PageRequest.of(0, 1,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals("Two", page.getContent().get(0).getFavorite().getBusiness().getName());

        assertTrue(items.findByListAndFavorite(a.getId(), f1.getId()).isPresent());
        assertFalse(items.findByListAndFavorite(empty.getId(), f1.getId()).isPresent());

        items.deleteByList(a.getId());
        em.clear();
        assertEquals(0, items.countActiveByList(a.getId()));
        assertEquals(1, items.countActiveByList(b.getId()));
    }
}
