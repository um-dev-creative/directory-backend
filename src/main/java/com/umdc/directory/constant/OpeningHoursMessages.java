package com.umdc.directory.constant;

/// Reasons returned in the `message-error` header by the opening hours endpoints, and their limits.
public final class OpeningHoursMessages {

    public static final String BODY_REQUIRED = "Request body is required";
    public static final String DAY_RANGE = "day_of_week must be between 1 (Monday) and 7 (Sunday)";
    public static final String TIME_FORMAT = "opens_at and closes_at must use the HH:mm format (00:00 to 23:59)";
    public static final String TIMES_REQUIRED = "An open slot needs opens_at and closes_at";
    public static final String CLOSED_WITH_TIMES = "A closed day cannot have opens_at or closes_at";
    public static final String OPENS_BEFORE_CLOSES = "opens_at must be before closes_at; for a slot past midnight use a slot until 23:59 and another on the next day";
    public static final String CLOSED_MIXED = "A closed day cannot have other slots";
    public static final String OVERLAP = "Slots of the same day cannot overlap";
    public static final String TOO_MANY_SLOTS = "A day has at most 4 slots";
    public static final String INVALID_AT = "at must be an ISO-8601 instant, e.g. 2026-11-23T15:00:00Z";
    public static final int MAX_SLOTS_PER_DAY = 4;
    /// Time zone used when the business has none (or its name is not a known zone).
    public static final String DEFAULT_ZONE = "America/Toronto";

    private OpeningHoursMessages() {
    }
}
