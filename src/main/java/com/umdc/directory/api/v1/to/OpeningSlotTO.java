package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

/// One slot of the opening hours.
///
/// @param dayOfWeek ISO day, 1 = Monday ... 7 = Sunday
/// @param opensAt   local opening time, `HH:mm` (null when the day is closed)
/// @param closesAt  local closing time, `HH:mm` (null when the day is closed); `23:59` means "until the end of the day"
/// @param closed    true for a day on which the location is closed
public record OpeningSlotTO(
        @JsonProperty("day_of_week") int dayOfWeek,
        @JsonProperty("opens_at") String opensAt,
        @JsonProperty("closes_at") String closesAt,
        @JsonProperty("closed") boolean closed) {
}
