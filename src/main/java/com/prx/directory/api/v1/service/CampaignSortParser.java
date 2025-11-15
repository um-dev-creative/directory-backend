package com.prx.directory.api.v1.service;

import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for parsing campaign sort parameters.
 */
public final class CampaignSortParser {

    private static final String DEFAULT_SORT_FIELD = "createdDate";

    private static final Map<String, String> SORT_FIELD_MAPPING = Map.of(
            "name", "name",
            "start_date", "startDate",
            "end_date", "endDate",
            "created_date", DEFAULT_SORT_FIELD
    );

    private CampaignSortParser() {
        // Utility class
    }

    /**
     * Parses a sort string like "name,-start_date,created_date" into a Sort object.
     * Returns null if the sort string is invalid.
     * Returns default sort by created_date DESC if sort is null or blank.
     *
     * @param sort comma-separated list of sort fields, prefix with '-' for descending
     * @return Sort object or null if invalid
     */
    public static Sort parse(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Order.desc(DEFAULT_SORT_FIELD));
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String token : sort.split(",")) {
            String t = token.trim();
            if (t.isEmpty()) {
                return null;
            }

            boolean desc = t.startsWith("-");
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

