package com.umdc.directory.api.v1.to;

import java.time.LocalDate;

/// Views of a business on one UTC day.
///
/// @param date  the day (ISO, `yyyy-MM-dd`)
/// @param views number of views that day (0 when there were none)
public record DailyViewsTO(LocalDate date, long views) {
}
