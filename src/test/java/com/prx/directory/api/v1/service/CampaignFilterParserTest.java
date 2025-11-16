package com.prx.directory.api.v1.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CampaignFilterParserTest {

    private final CampaignFilterParser parser = new CampaignFilterParser();

    @Test
    void parseName_nullOrEmpty() {
        assertNull(parser.parseName(null));
        assertNull(parser.parseName(Map.of()));
    }

    @Test
    void parseName_legacyKey() {
        Map<String, String> m = new HashMap<>();
        m.put("q", "TestName");
        assertEquals("TestName", parser.parseName(m));
    }

    @Test
    void parseName_tooLong_throws() {
        Map<String,String> m = new HashMap<>();
        m.put("name", "x".repeat(121));
        assertThrows(IllegalArgumentException.class, () -> parser.parseName(m));
    }

    @Test
    void parseUUIDs_validAndLegacy() {
        UUID id = UUID.randomUUID();
        Map<String,String> m = new HashMap<>();
        m.put("category_fk", id.toString());
        assertEquals(id, parser.parseCategoryId(m));
        m.clear();
        m.put("category_id", id.toString());
        assertEquals(id, parser.parseCategoryId(m));
    }

    @Test
    void parseActive_valid() {
        Map<String,String> m = new HashMap<>();
        m.put("active", "true");
        assertEquals(Boolean.TRUE, parser.parseActive(m));

        m.put("active", "false");
        assertEquals(Boolean.FALSE, parser.parseActive(m));
    }

    @Test
    void parseActive_invalid_throws() {
        Map<String,String> m = new HashMap<>();
        m.put("active", "notabool");
        assertThrows(IllegalArgumentException.class, () -> parser.parseActive(m));
    }

    @Test
    void parseInstants_validAndLegacy() {
        Map<String,String> m = new HashMap<>();
        m.put("start_from", "2025-11-01T00:00:00Z");
        assertEquals(Instant.parse("2025-11-01T00:00:00Z"), parser.parseStartFrom(m));

        m.clear();
        m.put("start_date_from", "2025-11-02T00:00:00Z");
        assertEquals(Instant.parse("2025-11-02T00:00:00Z"), parser.parseStartFrom(m));
    }

    @Test
    void validateDateRanges_invalidStart_throws() {
        Instant a = Instant.parse("2025-11-03T00:00:00Z");
        Instant b = Instant.parse("2025-11-02T00:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> parser.validateDateRanges(a, b, null, null));
    }

    @Test
    void validateDateRanges_invalidEnd_throws() {
        Instant a = Instant.parse("2025-11-03T00:00:00Z");
        Instant b = Instant.parse("2025-11-02T00:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> parser.validateDateRanges(null, null, a, b));
    }
}
