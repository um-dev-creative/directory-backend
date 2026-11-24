package com.umdc.directory.jpa.repository;

import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.entity.CategoryTranslationEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cattransdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=USER;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS directory_site\\\\;CREATE SCHEMA IF NOT EXISTS general\\\\;"
                + "CREATE ALIAS IF NOT EXISTS directory_site.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"\\\\;"
                + "CREATE ALIAS IF NOT EXISTS general.uuid_generate_v4 FOR \"java.util.UUID.randomUUID\"",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=test"
})
class CategoryTranslationRepositoryTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private CategoryTranslationRepository repository;

    private UUID category(String name) {
        var c = new CategoryEntity();
        c.setName(name);
        c.setDescription(name);
        c.setActive(true);
        c.setCreatedDate(LocalDateTime.now());
        c.setLastUpdate(LocalDateTime.now());
        em.persist(c);
        return c.getId();
    }

    @Test
    void findsOnlyTheRequestedLocaleForTheGivenCategories() {
        UUID a = category("Food");
        UUID b = category("Health");
        repository.save(new CategoryTranslationEntity(a, "es", "Comida"));
        repository.save(new CategoryTranslationEntity(a, "fr-CA", "Nourriture"));
        repository.save(new CategoryTranslationEntity(b, "es", "Salud"));
        repository.flush();

        List<CategoryTranslationEntity> es = repository.findByCategoryFkInAndLocale(List.of(a, b), "es");
        assertEquals(2, es.size());
        assertTrue(es.stream().anyMatch(t -> t.getName().equals("Comida") && t.getCategoryFk().equals(a)));
        assertEquals(1, repository.findByCategoryFkInAndLocale(List.of(a), "fr-CA").size());
        assertTrue(repository.findByCategoryFkInAndLocale(List.of(b), "fr-CA").isEmpty());
    }

    @Test
    void categoryAndLocaleAreUnique() {
        UUID a = category("Food");
        repository.saveAndFlush(new CategoryTranslationEntity(a, "es", "Comida"));
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(new CategoryTranslationEntity(a, "es", "Otra")));
    }

    @Test
    void gettersAndSetters() {
        var t = new CategoryTranslationEntity();
        UUID id = UUID.randomUUID();
        UUID cat = UUID.randomUUID();
        t.setId(id);
        t.setCategoryFk(cat);
        t.setLocale("es");
        t.setName("X");
        assertEquals(id, t.getId());
        assertEquals(cat, t.getCategoryFk());
        assertEquals("es", t.getLocale());
        assertEquals("X", t.getName());
        assertNotNull(t);
    }
}
