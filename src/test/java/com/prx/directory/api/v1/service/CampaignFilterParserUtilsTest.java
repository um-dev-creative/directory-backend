package com.prx.directory.api.v1.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CampaignFilterParserUtils - unit tests for utility parsing methods")
class CampaignFilterParserUtilsTest {

    @Test
    @DisplayName("valueOr with primaries picks first non-blank primary")
    void valueOrPrimariesPicksPrimary() {
        Map<String, String> map = new HashMap<>();
        map.put("p1", "value1");
        map.put("legacy", "legacyValue");

        String result = CampaignFilterParserUtils.valueOr(map, new String[]{"p1", "p2"}, "legacy");
        assertEquals("value1", result);
    }

    @Test
    @DisplayName("valueOr falls back to legacy when primaries missing or blank")
    void valueOrFallsBackToLegacy() {
        Map<String, String> map = new HashMap<>();
        map.put("p1", "  ");
        map.put("legacy", "legacyValue");

        String result = CampaignFilterParserUtils.valueOr(map, new String[]{"p1", "p2"}, "legacy");
        assertEquals("legacyValue", result);
    }

    @Test
    @DisplayName("valueOr single primary overload returns legacy when primary blank")
    void valueOrSinglePrimary() {
        Map<String, String> map = new HashMap<>();
        map.put("primary", "");
        map.put("legacy", "L");

        String res = CampaignFilterParserUtils.valueOr(map, "primary", "legacy");
        assertEquals("L", res);
    }

    @Test
    @DisplayName("parseUUID returns null for blank or null")
    void parseUUID_nullBlank() {
        assertNull(CampaignFilterParserUtils.parseUUID(null));
        assertNull(CampaignFilterParserUtils.parseUUID("  "));
    }

    @Test
    @DisplayName("parseUUID parses valid uuid")
    void parseUUID_valid() {
        UUID u = UUID.randomUUID();
        assertEquals(u, CampaignFilterParserUtils.parseUUID(u.toString()));
    }

    @Test
    @DisplayName("parseBoolean handles true/false and case-insensitive")
    void parseBoolean_variants() {
        assertEquals(Boolean.TRUE, CampaignFilterParserUtils.parseBoolean("true"));
        assertEquals(Boolean.FALSE, CampaignFilterParserUtils.parseBoolean("false"));
        assertEquals(Boolean.TRUE, CampaignFilterParserUtils.parseBoolean("TrUe"));
    }

    @Test
    @DisplayName("parseBoolean throws for invalid values")
    void parseBoolean_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CampaignFilterParserUtils.parseBoolean("notabool"));
    }

    @Test
    @DisplayName("parseToInstant parses ISO_INSTANT, OffsetDateTime and LocalDateTime")
    void parseToInstant_variousFormats() {
        Instant i1 = CampaignFilterParserUtils.parseToInstant("2025-11-01T00:00:00Z");
        assertEquals(Instant.parse("2025-11-01T00:00:00Z"), i1);

        Instant i2 = CampaignFilterParserUtils.parseToInstant("2025-11-01T00:00:00+01:00");
        assertEquals(java.time.OffsetDateTime.parse("2025-11-01T00:00:00+01:00").toInstant(), i2);

        Instant i3 = CampaignFilterParserUtils.parseToInstant("2025-11-01T00:00:00");
        assertEquals(LocalDateTime.parse("2025-11-01T00:00:00").toInstant(java.time.ZoneOffset.UTC), i3);
    }

    @Test
    @DisplayName("parseToInstant throws IllegalArgumentException for invalid input")
    void parseToInstant_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CampaignFilterParserUtils.parseToInstant("not-a-date"));
    }
}

