package com.prx.directory.client.mercury.to;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VerificationCodeRequestTest {

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    @Test
    @DisplayName("VerificationCodeRequest: valid request produces no violations")
    void validRequest_noViolations() {
        VerificationCodeRequest req = new VerificationCodeRequest(UUID.randomUUID(), UUID.randomUUID(), "123456789");
        Set<?> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("VerificationCodeRequest: invalid code length results in violations")
    void invalidCodeLength_violations() {
        VerificationCodeRequest req = new VerificationCodeRequest(UUID.randomUUID(), UUID.randomUUID(), "123");
        Set<?> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("VerificationCodeRequest: null fields result in violations")
    void nullFields_violations() {
        VerificationCodeRequest req = new VerificationCodeRequest(null, null, null);
        Set<?> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }
}
