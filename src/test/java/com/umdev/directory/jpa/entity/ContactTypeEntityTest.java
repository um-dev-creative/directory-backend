package com.umdev.directory.jpa.entity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContactTypeEntityTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void defaultConstructor_setsActiveToFalse() {
        ContactTypeEntity entity = new ContactTypeEntity();
        assertFalse(entity.getActive());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        ContactTypeEntity entity = new ContactTypeEntity();
        UUID id = UUID.randomUUID();
        entity.setId(id);
        entity.setName("Email");
        entity.setDescription("Email contact type");
        entity.setActive(true);

        assertEquals(id, entity.getId());
        assertEquals("Email", entity.getName());
        assertEquals("Email contact type", entity.getDescription());
        assertTrue(entity.getActive());
    }

    @Test
    void validation_nullName_causesViolation() {
        ContactTypeEntity entity = new ContactTypeEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(null);
        entity.setActive(true);

        var violations = validator.validate(entity);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void validation_nameTooLong_causesViolation() {
        ContactTypeEntity entity = new ContactTypeEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("x".repeat(21));
        entity.setActive(true);

        var violations = validator.validate(entity);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void validation_descriptionTooLong_causesViolation() {
        ContactTypeEntity entity = new ContactTypeEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Email");
        entity.setDescription("x".repeat(251));
        entity.setActive(true);

        var violations = validator.validate(entity);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));
    }

    @Test
    void validation_validEntity_noViolations() {
        ContactTypeEntity entity = new ContactTypeEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Phone");
        entity.setDescription("Phone contact");
        entity.setActive(true);

        var violations = validator.validate(entity);
        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_nullDescription_isAllowed() {
        ContactTypeEntity entity = new ContactTypeEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("SMS");
        entity.setDescription(null);
        entity.setActive(false);

        var violations = validator.validate(entity);
        assertTrue(violations.isEmpty());
    }
}

