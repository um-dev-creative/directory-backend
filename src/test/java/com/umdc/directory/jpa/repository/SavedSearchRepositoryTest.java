package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.LastSearchEntity;
import com.umdc.directory.jpa.entity.SavedSearchEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Runs the derived queries of the saved searches against H2 in PostgreSQL mode.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:savedsearchdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=USER;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS directory_site\\\\;CREATE SCHEMA IF NOT EXISTS general\\\\;"
                + "CREATE ALIAS IF NOT EXISTS directory_site.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"\\\\;"
                + "CREATE ALIAS IF NOT EXISTS general.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=test"
})
class SavedSearchRepositoryTest {

    @Autowired
    private SavedSearchRepository saved;
    @Autowired
    private LastSearchRepository last;

    private SavedSearchEntity row(UUID user, String name, LocalDateTime created) {
        var e = new SavedSearchEntity();
        e.setUserFk(user);
        e.setName(name);
        e.setQ("q");
        e.setCreatedAt(created);
        e.setUpdatedAt(created);
        return saved.saveAndFlush(e);
    }

    @Test
    void queriesAreScopedToTheOwner() {
        var me = UUID.randomUUID();
        var other = UUID.randomUUID();
        var now = LocalDateTime.now();
        var older = row(me, "Pizza", now.minusDays(1));
        var newer = row(me, "Sushi", now);
        var foreign = row(other, "Pizza", now);

        var list = saved.findByUserFkOrderByCreatedAtDesc(me);
        assertEquals(2, list.size());
        assertEquals(newer.getId(), list.get(0).getId());
        assertEquals(2, saved.countByUserFk(me));
        assertTrue(saved.findByIdAndUserFk(older.getId(), me).isPresent());
        assertFalse(saved.findByIdAndUserFk(foreign.getId(), me).isPresent());
        assertTrue(saved.existsByUserFkAndNameIgnoreCase(me, "pizza"));
        assertFalse(saved.existsByUserFkAndNameIgnoreCase(me, "tacos"));
        assertFalse(saved.existsByUserFkAndNameIgnoreCaseAndIdNot(me, "pizza", older.getId()));
        assertTrue(saved.existsByUserFkAndNameIgnoreCaseAndIdNot(me, "pizza", newer.getId()));
    }

    @Test
    void lastSearchIsOneRowPerUser() {
        var user = UUID.randomUUID();
        var e = new LastSearchEntity();
        e.setUserFk(user);
        e.setQ("a");
        e.setUpdatedAt(LocalDateTime.now());
        last.saveAndFlush(e);
        var again = last.findById(user).orElseThrow();
        again.setQ("b");
        last.saveAndFlush(again);
        assertEquals(1, last.count());
        assertEquals("b", last.findById(user).orElseThrow().getQ());
    }
}
