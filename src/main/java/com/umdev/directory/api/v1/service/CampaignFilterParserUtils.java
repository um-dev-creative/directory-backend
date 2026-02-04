package com.umdev.directory.api.v1.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * Small utility class extracted from CampaignFilterParser to keep that class focused.
 */
final class CampaignFilterParserUtils {

    private CampaignFilterParserUtils() {
        // utility
    }

    static String valueOr(Map<String, String> map, String[] primaries, String legacy) {
        if (map == null) return null;
        for (String primary : primaries) {
            String v = map.get(primary);
            if (v != null && !v.isBlank()) return v;
        }
        String v = map.get(legacy);
        return (v == null || v.isBlank()) ? null : v;
    }

    static String valueOr(Map<String, String> map, String primary, String legacy) {
        if (map == null) return null;
        String v = map.get(primary);
        return (v == null || v.isBlank()) ? map.get(legacy) : v;
    }

    static UUID parseUUID(String v) {
        if (v == null || v.isBlank()) return null;
        return UUID.fromString(v.trim());
    }

    static Boolean parseBoolean(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        String t = v.trim().toLowerCase(Locale.ROOT);
        if (TRUE.toString().equals(t)) return TRUE;
        if (FALSE.toString().equals(t)) return FALSE;
        throw new IllegalArgumentException("invalid boolean value");
    }

    static Instant parseToInstant(String v) {
        if (v == null || v.isBlank()) return null;
        String trimmed = v.trim();
        // Try ISO_INSTANT first (e.g., 2025-11-01T00:00:00Z)
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // continue
        }
        // Try OffsetDateTime (e.g., 2025-11-01T00:00:00+01:00)
        try {
            return OffsetDateTime.parse(trimmed).toInstant();
        } catch (DateTimeParseException ignored) {
            // continue
        }
        // Try LocalDateTime (no zone), treat as UTC
        try {
            LocalDateTime ldt = LocalDateTime.parse(trimmed);
            return ldt.toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + v, e);
        }
    }
}

