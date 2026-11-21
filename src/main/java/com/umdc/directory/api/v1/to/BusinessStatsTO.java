package com.umdc.directory.api.v1.to;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/// Summary of the views of a business. Days are UTC; there is no personal data in it.
///
/// @param businessId       the business
/// @param from             first day of `daily`
/// @param to               last day of `daily` (today)
/// @param totalViews       views in `from`..`to`
/// @param viewsToday       views today
/// @param viewsLast7Days   views in the last 7 days, today included (whatever the requested window is)
/// @param daily            one entry per day from `from` to `to`, zeros included, oldest first
public record BusinessStatsTO(
        UUID businessId,
        LocalDate from,
        LocalDate to,
        long totalViews,
        long viewsToday,
        long viewsLast7Days,
        List<DailyViewsTO> daily) {
}
