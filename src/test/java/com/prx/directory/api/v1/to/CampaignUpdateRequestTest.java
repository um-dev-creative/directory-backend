package com.prx.directory.api.v1.to;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignUpdateRequestTest {

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
    void validRequest_noViolations() {
        CampaignUpdateRequest req = new CampaignUpdateRequest(null, null, null, null, null, null, null, null, null, null);
        Set<ConstraintViolation<CampaignUpdateRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Expected no validation violations for null/optional fields");
        assertTrue(req.isStartBeforeOrEqualEnd(), "When dates are null the method should return true");
    }

    @Test
    void nameTooLong_causesViolation() {
        String longName = "x".repeat(121);
        CampaignUpdateRequest req = new CampaignUpdateRequest(longName, null, null, null, null, null, null, null, null, null);
        Set<ConstraintViolation<CampaignUpdateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        boolean found = violations.stream().anyMatch(v -> v.getMessage().contains("title must not exceed"));
        assertTrue(found, "Expected a title length violation");
    }

    @Test
    void descriptionTooLong_causesViolation() {
        String longDesc = "x".repeat(1201);
        CampaignUpdateRequest req = new CampaignUpdateRequest(null, longDesc, null, null, null, null, null, null, null, null);
        Set<ConstraintViolation<CampaignUpdateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        boolean found = violations.stream().anyMatch(v -> v.getMessage().contains("description must not exceed"));
        assertTrue(found, "Expected a description length violation");
    }

    @Test
    void startAfterEnd_methodAndValidationFail() {
        LocalDateTime start = LocalDateTime.parse("2025-11-10T00:00:00");
        LocalDateTime end = LocalDateTime.parse("2025-11-02T00:00:00");
        CampaignUpdateRequest req = new CampaignUpdateRequest(null, null, start, end, null, null, null, null, null, null);

        // method should return false
        assertFalse(req.isStartBeforeOrEqualEnd());

        // validation should pick up the AssertTrue annotated method
        Set<ConstraintViolation<CampaignUpdateRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        boolean found = violations.stream().anyMatch(v -> v.getMessage().contains("startDate must be before or equal to endDate"));
        assertTrue(found, "Expected a start/end date validation violation");
    }

    @Test
    void startEqualsEnd_methodAndValidationPass() {
        LocalDateTime localDateTime = LocalDateTime.parse("2025-11-02T00:00:00");
        CampaignUpdateRequest req = new CampaignUpdateRequest(null, null, localDateTime, localDateTime, null, null, null, null, null, null);
        assertTrue(req.isStartBeforeOrEqualEnd());
        Set<ConstraintViolation<CampaignUpdateRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void startBeforeEnd_methodAndValidationPass() {
        LocalDateTime start = LocalDateTime.parse("2025-11-01T00:00:00");
        LocalDateTime end = LocalDateTime.parse("2025-11-02T00:00:00");
        CampaignUpdateRequest req = new CampaignUpdateRequest(null, null, start, end, null, null, null, null, null, null);
        assertTrue(req.isStartBeforeOrEqualEnd());
        Set<ConstraintViolation<CampaignUpdateRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void nullNameAndDescription_areAllowed() {
        CampaignUpdateRequest req = new CampaignUpdateRequest(null, null, null, null, null, null, null, null, null, null);
        Set<ConstraintViolation<CampaignUpdateRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }
}
