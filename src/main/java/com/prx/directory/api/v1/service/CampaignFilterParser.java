package com.prx.directory.api.v1.service;

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
 * Utility class for parsing and validating campaign filter parameters.
 */
public class CampaignFilterParser {

    private static final int MAX_NAME_LENGTH = 120;

    /**
     * Default constructor for CampaignFilterParser.
     * Initializes a new instance of the CampaignFilterParser utility class.
     */
    public CampaignFilterParser() {
        // Default constructor
    }

    public String parseName(Map<String, String> filters) {
        String name = valueOr(filters, "name", "q");
        if (name != null && name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("name exceeds maximum length");
        }
        return name;
    }

    public UUID parseCategoryId(Map<String, String> filters) {
        return parseUUID(valueOr(filters, "category_fk", "category_id"));
    }

    public UUID parseBusinessId(Map<String, String> filters) {
        return parseUUID(valueOr(filters, "business_fk", "business_id"));
    }

    public Boolean parseActive(Map<String, String> filters) {
        return parseBoolean(filters.get("active"));
    }

    // Date filter parsers now return Instant
    public Instant parseStartFrom(Map<String, String> filters) {
        return parseToInstant(valueOr(filters, "start_from", "start_date_from"));
    }

    public Instant parseStartTo(Map<String, String> filters) {
        return parseToInstant(valueOr(filters, "start_to", "start_date_to"));
    }

    public Instant parseEndFrom(Map<String, String> filters) {
        return parseToInstant(valueOr(filters, "end_from", "end_date_from"));
    }

    public Instant parseEndTo(Map<String, String> filters) {
        return parseToInstant(valueOr(filters, "end_to", "end_date_to"));
    }

    public void validateDateRanges(LocalDateTime startFrom, LocalDateTime startTo, LocalDateTime endFrom, LocalDateTime endTo) {
        if (startFrom != null && startTo != null && startFrom.isAfter(startTo)) {
            throw new IllegalArgumentException("start date range is invalid");
        }
        if (endFrom != null && endTo != null && endFrom.isAfter(endTo)) {
            throw new IllegalArgumentException("end date range is invalid");
        }
    }

    private String valueOr(Map<String, String> map, String primary, String legacy) {
        if (map == null) return null;
        String v = map.get(primary);
        return (v == null || v.isBlank()) ? map.get(legacy) : v;
    }

    private UUID parseUUID(String v) {
        if (v == null || v.isBlank()) return null;
        return UUID.fromString(v.trim());
    }

    private Boolean parseBoolean(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        String t = v.trim().toLowerCase(Locale.ROOT);
        if (TRUE.toString().equals(t)) return TRUE;
        if (FALSE.toString().equals(t)) return FALSE;
        throw new IllegalArgumentException("invalid boolean value");
    }

    private Instant parseToInstant(String v) {
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
