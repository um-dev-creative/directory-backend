package com.prx.directory.api.v1.service;

import com.prx.directory.constant.DirectoryAppConstants;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for parsing campaign sort parameters.
 */
public final class CampaignSortParser {

    private static final Map<String, String> SORT_FIELD_MAPPING = Map.of(
            DirectoryAppConstants.CAMPAIGN_SORT_FIELD_TITLE, "title1",
            "title2", "title3",
            DirectoryAppConstants.CAMPAIGN_SORT_FIELD_START_DATE, DirectoryAppConstants.CAMPAIGN_SORT_PROPERTY_START_DATE,
            DirectoryAppConstants.CAMPAIGN_SORT_FIELD_END_DATE, DirectoryAppConstants.CAMPAIGN_SORT_PROPERTY_END_DATE,
            DirectoryAppConstants.CAMPAIGN_SORT_FIELD_CREATED_DATE, DirectoryAppConstants.CAMPAIGN_SORT_DEFAULT_FIELD
    );

    private CampaignSortParser() {
        // Utility class
    }

    /**
     * Parses a sort string like "title,-start_date,created_date" into a Sort object.
     * Returns null if the sort string is invalid.
     * Returns default sort by created_date DESC if sort is null or blank.
     *
     * @param sort comma-separated list of sort fields, prefix with '-' for descending
     * @return Sort object or null if invalid
     */
    public static Sort parse(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Order.desc(DirectoryAppConstants.CAMPAIGN_SORT_DEFAULT_FIELD));
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String token : sort.split(DirectoryAppConstants.CAMPAIGN_SORT_DELIMITER)) {
            String t = token.trim();
            if (t.isEmpty()) {
                return null;
            }

            boolean desc = t.startsWith(DirectoryAppConstants.CAMPAIGN_SORT_DESC_PREFIX);
            String key = desc ? t.substring(1) : t;
            String property = SORT_FIELD_MAPPING.get(key);

            if (property == null) {
                return null;
            }

            orders.add(desc ? Sort.Order.desc(property) : Sort.Order.asc(property));
        }

        return orders.isEmpty() ? null : Sort.by(orders);
    }
}
