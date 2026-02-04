package com.umdev.directory.api.v1.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CampaignFilterParserTest {

    private CampaignFilterParser parser;

    @BeforeEach
    void setup() {
        parser = new CampaignFilterParser();
    }

    // -------- parseName tests --------

    @Test
    void parseName_withValidName_returnsName() {
        Map<String, String> filters = Map.of("title", "Holiday Sale");
        assertEquals("Holiday Sale", parser.parseName(filters));
    }

    @Test
    void parseName_withLegacyQ_returnsQ() {
        Map<String, String> filters = Map.of("q", "Search Term");
        assertEquals("Search Term", parser.parseName(filters));
    }

    @Test
    void parseName_prefersPrimaryOverLegacy() {
        Map<String, String> filters = new HashMap<>();
        filters.put("name", "Primary");
        filters.put("q", "Legacy");
        assertEquals("Primary", parser.parseName(filters));
    }

    @Test
    void parseName_exceedsMaxLength_throwsException() {
        String longTitle = "x".repeat(121);
        Map<String, String> filters = Map.of("title", longTitle);
        assertThrows(IllegalArgumentException.class, () -> parser.parseName(filters));
    }

    @Test
    void parseName_nullMap_returnsNull() {
        assertNull(parser.parseName(null));
    }

    @Test
    void parseName_emptyMap_returnsNull() {
        assertNull(parser.parseName(Map.of()));
    }

    // -------- parseCategoryId tests --------

    @Test
    void parseCategoryId_withValidUUID_returnsUUID() {
        UUID uuid = UUID.randomUUID();
        Map<String, String> filters = Map.of("category_fk", uuid.toString());
        assertEquals(uuid, parser.parseCategoryId(filters));
    }

    @Test
    void parseCategoryId_withLegacyKey_returnsUUID() {
        UUID uuid = UUID.randomUUID();
        Map<String, String> filters = Map.of("category_id", uuid.toString());
        assertEquals(uuid, parser.parseCategoryId(filters));
    }

    @Test
    void parseCategoryId_invalidUUID_throwsException() {
        Map<String, String> filters = Map.of("category_fk", "not-a-uuid");
        assertThrows(IllegalArgumentException.class, () -> parser.parseCategoryId(filters));
    }

    @Test
    void parseCategoryId_nullValue_returnsNull() {
        assertNull(parser.parseCategoryId(Map.of()));
    }

    // -------- parseBusinessId tests --------

    @Test
    void parseBusinessId_withValidUUID_returnsUUID() {
        UUID uuid = UUID.randomUUID();
        Map<String, String> filters = Map.of("business_fk", uuid.toString());
        assertEquals(uuid, parser.parseBusinessId(filters));
    }

    @Test
    void parseBusinessId_withLegacyKey_returnsUUID() {
        UUID uuid = UUID.randomUUID();
        Map<String, String> filters = Map.of("business_id", uuid.toString());
        assertEquals(uuid, parser.parseBusinessId(filters));
    }

    @Test
    void parseBusinessId_blankValue_returnsNull() {
        Map<String, String> filters = Map.of("business_fk", "   ");
        assertNull(parser.parseBusinessId(filters));
    }

    // -------- parseActive tests --------

    @Test
    void parseActive_withTrue_returnsTrue() {
        Map<String, String> filters = Map.of("active", "true");
        assertTrue(parser.parseActive(filters));
    }

    @Test
    void parseActive_withFalse_returnsFalse() {
        Map<String, String> filters = Map.of("active", "false");
        assertFalse(parser.parseActive(filters));
    }

    @Test
    void parseActive_caseInsensitive_works() {
        assertEquals(Boolean.TRUE, parser.parseActive(Map.of("active", "TRUE")));
        assertEquals(Boolean.FALSE, parser.parseActive(Map.of("active", "FALSE")));
        assertEquals(Boolean.TRUE, parser.parseActive(Map.of("active", "TrUe")));
    }

    @Test
    void parseActive_invalidValue_throwsException() {
        Map<String, String> filters = Map.of("active", "notabool");
        assertThrows(IllegalArgumentException.class, () -> parser.parseActive(filters));
    }

    @Test
    void parseActive_nullValue_returnsNull() {
        assertNull(parser.parseActive(Map.of()));
    }

    @Test
    void parseActive_blankValue_returnsNull() {
        Map<String, String> filters = Map.of("active", "  ");
        assertNull(parser.parseActive(filters));
    }

    // -------- parseStartFrom tests --------

    @Test
    void parseStartFrom_validISO_returnsInstant() {
        Map<String, String> filters = Map.of("start_from", "2025-11-01T00:00:00Z");
        Instant expected = Instant.parse("2025-11-01T00:00:00Z");
        assertEquals(expected, parser.parseStartFrom(filters));
    }

    @Test
    void parseStartFrom_withLegacyKey_returnsInstant() {
        Map<String, String> filters = Map.of("start_date_from", "2025-11-01T00:00:00Z");
        Instant expected = Instant.parse("2025-11-01T00:00:00Z");
        assertEquals(expected, parser.parseStartFrom(filters));
    }

    @Test
    void parseStartFrom_invalidISO_throwsException() {
        Map<String, String> filters = Map.of("start_from", "not-a-date");
        assertThrows(Exception.class, () -> parser.parseStartFrom(filters));
    }

    @Test
    void parseStartFrom_nullValue_returnsNull() {
        assertNull(parser.parseStartFrom(Map.of()));
    }

    // -------- parseStartTo tests --------

    @Test
    void parseStartTo_validISO_returnsInstant() {
        Map<String, String> filters = Map.of("start_to", "2025-11-30T23:59:59Z");
        Instant expected = Instant.parse("2025-11-30T23:59:59Z");
        assertEquals(expected, parser.parseStartTo(filters));
    }

    @Test
    void parseStartTo_withLegacyKey_returnsInstant() {
        Map<String, String> filters = Map.of("start_date_to", "2025-11-30T23:59:59Z");
        Instant expected = Instant.parse("2025-11-30T23:59:59Z");
        assertEquals(expected, parser.parseStartTo(filters));
    }

    // -------- parseEndFrom tests --------

    @Test
    void parseEndFrom_validISO_returnsInstant() {
        Map<String, String> filters = Map.of("end_from", "2025-12-01T00:00:00Z");
        Instant expected = Instant.parse("2025-12-01T00:00:00Z");
        assertEquals(expected, parser.parseEndFrom(filters));
    }

    @Test
    void parseEndFrom_withLegacyKey_returnsInstant() {
        Map<String, String> filters = Map.of("end_date_from", "2025-12-01T00:00:00Z");
        Instant expected = Instant.parse("2025-12-01T00:00:00Z");
        assertEquals(expected, parser.parseEndFrom(filters));
    }

    // -------- parseEndTo tests --------

    @Test
    void parseEndTo_validISO_returnsInstant() {
        Map<String, String> filters = Map.of("end_to", "2025-12-31T23:59:59Z");
        Instant expected = Instant.parse("2025-12-31T23:59:59Z");
        assertEquals(expected, parser.parseEndTo(filters));
    }

    @Test
    void parseEndTo_withLegacyKey_returnsInstant() {
        Map<String, String> filters = Map.of("end_date_to", "2025-12-31T23:59:59Z");
        Instant expected = Instant.parse("2025-12-31T23:59:59Z");
        assertEquals(expected, parser.parseEndTo(filters));
    }

    // -------- validateDateRanges tests --------

    @Test
    void validateDateRanges_validRanges_doesNotThrow() {
        LocalDateTime start1 = LocalDateTime.parse("2025-11-01T00:00:00");
        LocalDateTime start2 = LocalDateTime.parse("2025-11-30T23:59:59");
        LocalDateTime end1 = LocalDateTime.parse("2025-12-01T00:00:00");
        LocalDateTime end2 = LocalDateTime.parse("2025-12-31T23:59:59");

        assertDoesNotThrow(() -> parser.validateDateRanges(start1, start2, end1, end2));
    }

    @Test
    void validateDateRanges_startFromAfterStartTo_throwsException() {
        LocalDateTime start1 = LocalDateTime.parse("2025-11-30T23:59:59");
        LocalDateTime start2 = LocalDateTime.parse("2025-11-01T00:00:00");

        assertThrows(IllegalArgumentException.class,
                () -> parser.validateDateRanges(start1, start2, null, null));
    }

    @Test
    void validateDateRanges_endFromAfterEndTo_throwsException() {
        LocalDateTime end1 = LocalDateTime.parse("2025-12-31T23:59:59");
        LocalDateTime end2 = LocalDateTime.parse("2025-12-01T00:00:00");

        assertThrows(IllegalArgumentException.class,
                () -> parser.validateDateRanges(null, null, end1, end2));
    }

    @Test
    void validateDateRanges_nullValues_doesNotThrow() {
        assertDoesNotThrow(() -> parser.validateDateRanges(null, null, null, null));
    }

    @Test
    void validateDateRanges_onlyStartFrom_doesNotThrow() {
        LocalDateTime start = LocalDateTime.parse("2025-11-01T00:00:00");
        assertDoesNotThrow(() -> parser.validateDateRanges(start, null, null, null));
    }

    @Test
    void validateDateRanges_equalDates_doesNotThrow() {
        LocalDateTime instant = LocalDateTime.parse("2025-11-01T00:00:00");
        assertDoesNotThrow(() -> parser.validateDateRanges(instant, instant, instant, instant));
    }
}
