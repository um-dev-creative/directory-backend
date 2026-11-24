package com.umdc.directory.api.v1.service;

import com.umdc.directory.constant.CampaignStatus;
import com.umdc.directory.constant.CampaignType;
import com.umdc.directory.constant.DirectoryAppConstants;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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
        String title = CampaignFilterParserUtils.valueOr(filters, new String[]{"name", "title"}, "q");
        if (title != null && title.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("title exceeds maximum length");
        }
        return title;
    }

    public UUID parseCategoryId(Map<String, String> filters) {
        return CampaignFilterParserUtils.parseUUID(CampaignFilterParserUtils.valueOr(filters, "category_fk", "category_id"));
    }

    public UUID parseBusinessId(Map<String, String> filters) {
        return CampaignFilterParserUtils.parseUUID(CampaignFilterParserUtils.valueOr(filters, "business_fk", "business_id"));
    }

    public Boolean parseActive(Map<String, String> filters) {
        return CampaignFilterParserUtils.parseBoolean(filters == null ? null : filters.get("active"));
    }

    /// Lifecycle status filter (`status`); null when absent, IllegalArgumentException when not a status.
    public CampaignStatus parseStatus(Map<String, String> filters) {
        String raw = filters == null ? null : filters.get("status");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return CampaignStatus.parse(raw)
                .orElseThrow(() -> new IllegalArgumentException(DirectoryAppConstants.CAMPAIGN_STATUS_INVALID));
    }

    /// Type filter (`type`), normalised to the enum name; null when absent, IllegalArgumentException when invalid.
    public String parseType(Map<String, String> filters) {
        String raw = filters == null ? null : filters.get("type");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return CampaignType.parse(raw)
                .orElseThrow(() -> new IllegalArgumentException(DirectoryAppConstants.CAMPAIGN_TYPE_INVALID)).name();
    }

    // Date filter parsers now return Instant
    public Instant parseStartFrom(Map<String, String> filters) {
        return CampaignFilterParserUtils.parseToInstant(CampaignFilterParserUtils.valueOr(filters, "start_from", "start_date_from"));
    }

    public Instant parseStartTo(Map<String, String> filters) {
        return CampaignFilterParserUtils.parseToInstant(CampaignFilterParserUtils.valueOr(filters, "start_to", "start_date_to"));
    }

    public Instant parseEndFrom(Map<String, String> filters) {
        return CampaignFilterParserUtils.parseToInstant(CampaignFilterParserUtils.valueOr(filters, "end_from", "end_date_from"));
    }

    public Instant parseEndTo(Map<String, String> filters) {
        return CampaignFilterParserUtils.parseToInstant(CampaignFilterParserUtils.valueOr(filters, "end_to", "end_date_to"));
    }

    public void validateDateRanges(LocalDateTime startFrom, LocalDateTime startTo, LocalDateTime endFrom, LocalDateTime endTo) {
        if (startFrom != null && startTo != null && startFrom.isAfter(startTo)) {
            throw new IllegalArgumentException("start date range is invalid");
        }
        if (endFrom != null && endTo != null && endFrom.isAfter(endTo)) {
            throw new IllegalArgumentException("end date range is invalid");
        }
    }
}
