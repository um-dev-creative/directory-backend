package com.umdc.directory.api.v1.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationValidatorBoundsTest {

    @AfterEach
    void reset() {
        LocationValidator.setBoundsEnforced(true);
    }

    private static java.util.Optional<String> check(String lat, String lon) {
        return LocationValidator.validateFields(null, "x", null, null,
                lat == null ? null : new BigDecimal(lat), lon == null ? null : new BigDecimal(lon));
    }

    @Test
    void rejectsNullIsland() {
        assertEquals(LocationValidator.LOCATION_COORDINATES_NULL_ISLAND, check("0", "0").orElseThrow());
        assertEquals(LocationValidator.LOCATION_COORDINATES_NULL_ISLAND, check("0.0", "-0.00").orElseThrow());
    }

    @Test
    void nullIslandStaysRejectedWhenBoundsAreOff() {
        LocationValidator.setBoundsEnforced(false);
        assertEquals(LocationValidator.LOCATION_COORDINATES_NULL_ISLAND, check("0", "0").orElseThrow());
    }

    @Test
    void rejectsOutsideCanada() {
        assertEquals(LocationValidator.LOCATION_COORDINATES_OUTSIDE_CANADA, check("48.85", "2.35").orElseThrow());
        assertEquals(LocationValidator.LOCATION_COORDINATES_OUTSIDE_CANADA, check("-33.9", "151.2").orElseThrow());
        assertEquals(LocationValidator.LOCATION_COORDINATES_OUTSIDE_CANADA, check("43.65", "79.38").orElseThrow());
    }

    @Test
    void acceptsInsideAndEdges() {
        assertTrue(check("43.65", "-79.38").isEmpty());
        assertTrue(check("41.6", "-141.1").isEmpty());
        assertTrue(check("83.2", "-52.5").isEmpty());
    }

    @Test
    void rejectsJustOutsideEdges() {
        assertTrue(check("41.59", "-79").isPresent());
        assertTrue(check("83.21", "-79").isPresent());
        assertTrue(check("50", "-141.11").isPresent());
        assertTrue(check("50", "-52.51").isEmpty());
        assertTrue(check("50", "-52.49").isPresent());
    }

    @Test
    void boundsCanBeDisabled() {
        LocationValidator.setBoundsEnforced(false);
        assertTrue(check("48.85", "2.35").isEmpty());
    }

    @Test
    void incompleteAndRangeRulesStillApply() {
        assertEquals(com.umdc.directory.constant.DirectoryAppConstants.LOCATION_COORDINATES_PAIR, check("45", null).orElseThrow());
        assertEquals(com.umdc.directory.constant.DirectoryAppConstants.LOCATION_COORDINATES_RANGE, check("91", "0").orElseThrow());
        assertTrue(check(null, null).isEmpty());
    }

    @Test
    void propertyDrivesTheValidator() {
        var props = new LocationBoundsProperties();
        assertTrue(props.isEnforced());
        props.setEnforced(false);
        assertEquals(false, LocationValidator.isBoundsEnforced());
        props.setEnforced(true);
        assertTrue(LocationValidator.isBoundsEnforced());
    }
}
