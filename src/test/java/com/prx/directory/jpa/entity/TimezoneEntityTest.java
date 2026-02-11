package com.prx.directory.jpa.entity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TimezoneEntityTest {

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
    @DisplayName("TimezoneEntity: default constructor creates entity")
    void defaultConstructor_createsEntity() {
        TimezoneEntity entity = new TimezoneEntity();
        assertNotNull(entity);
    }

    @Test
    @DisplayName("TimezoneEntity: setters and getters work correctly")
    void settersAndGetters_workCorrectly() {
        TimezoneEntity entity = new TimezoneEntity();
        UUID id = UUID.randomUUID();
        Duration offset = Duration.ofHours(5).plusMinutes(30);
        LocalDateTime now = LocalDateTime.now();

        entity.setId(id);
        entity.setName("Eastern Standard Time");
        entity.setUtcOffset(offset);
        entity.setAbbreviation("EST");
        entity.setCreatedAt(now);
        entity.setLastUpdated(now);

        assertEquals(id, entity.getId());
        assertEquals("Eastern Standard Time", entity.getName());
        assertEquals(offset, entity.getUtcOffset());
        assertEquals("EST", entity.getAbbreviation());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getLastUpdated());
    }

    @Test
    @DisplayName("TimezoneEntity: validation fails when name is null")
    void validation_nullName_causesViolation() {
        TimezoneEntity entity = new TimezoneEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(null);
        entity.setUtcOffset(Duration.ZERO);
        entity.setAbbreviation("UTC");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setLastUpdated(LocalDateTime.now());

        var violations = validator.validate(entity);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    @DisplayName("TimezoneEntity: validation fails when name too long")
    void validation_nameTooLong_causesViolation() {
        TimezoneEntity entity = new TimezoneEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("x".repeat(101));
        entity.setUtcOffset(Duration.ZERO);
        entity.setAbbreviation("UTC");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setLastUpdated(LocalDateTime.now());

        var violations = validator.validate(entity);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    @DisplayName("TimezoneEntity: validation fails when abbreviation too long")
    void validation_abbreviationTooLong_causesViolation() {
        TimezoneEntity entity = new TimezoneEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Test Timezone");
        entity.setUtcOffset(Duration.ZERO);
        entity.setAbbreviation("x".repeat(11));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setLastUpdated(LocalDateTime.now());

        var violations = validator.validate(entity);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("abbreviation")));
    }

    @Test
    @DisplayName("TimezoneEntity: valid entity has no validation violations")
    void validation_validEntity_noViolations() {
        TimezoneEntity entity = new TimezoneEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Pacific Standard Time");
        entity.setUtcOffset(Duration.ofHours(-8));
        entity.setAbbreviation("PST");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setLastUpdated(LocalDateTime.now());

        var violations = validator.validate(entity);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("TimezoneEntity: validation fails when utcOffset is null")
    void validation_nullUtcOffset_causesViolation() {
        TimezoneEntity entity = new TimezoneEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Test Timezone");
        entity.setUtcOffset(null);
        entity.setAbbreviation("TST");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setLastUpdated(LocalDateTime.now());

        var violations = validator.validate(entity);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("utcOffset")));
    }
}

