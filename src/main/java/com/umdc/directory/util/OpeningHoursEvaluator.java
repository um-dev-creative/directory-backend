package com.umdc.directory.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/// Pure evaluation of opening hours: no clock, no I/O. Hours are local time in the zone given; `23:59` as closing time means
/// "until the end of the day", so a slot until 23:59 followed by a slot from 00:00 on the next day is one continuous opening.
public final class OpeningHoursEvaluator {

    /// Days ahead that are scanned for the next change. A schedule open without a break for longer has no foreseeable change.
    static final int HORIZON_DAYS = 14;
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59);

    private OpeningHoursEvaluator() {
    }

    /// A slot of the weekly schedule.
    ///
    /// @param day    day of the week
    /// @param opens  opening time (ignored when closed)
    /// @param closes closing time (ignored when closed)
    /// @param closed true when the location is closed all day
    public record Slot(DayOfWeek day, LocalTime opens, LocalTime closes, boolean closed) {
    }

    /// A change of state.
    ///
    /// @param at      the instant of the change
    /// @param opening true when the location opens at `at`; false when it closes
    public record Change(Instant at, boolean opening) {
    }

    private record Interval(Instant start, Instant end) {
    }

    /// @return true when some open slot contains the instant (opening included, closing excluded)
    public static boolean isOpenNow(List<Slot> hours, ZoneId zone, Instant at) {
        return intervalsAround(hours, zone, at).stream().anyMatch(i -> contains(i, at));
    }

    /// Next opening (when closed) or next closing (when open), from `at`.
    ///
    /// @return empty when there are no open slots or the location stays open beyond the scanned horizon
    public static Optional<Change> nextChange(List<Slot> hours, ZoneId zone, Instant at) {
        var intervals = intervalsAround(hours, zone, at);
        for (var interval : intervals) {
            if (contains(interval, at)) {
                // an interval that reaches the end of the scan may continue forever
                return interval.end().isAfter(horizonEnd(zone, at).minusSeconds(1)) ? Optional.empty() : Optional.of(new Change(interval.end(), false));
            }
        }
        return intervals.stream().filter(i -> i.start().isAfter(at)).findFirst().map(i -> new Change(i.start(), true));
    }

    private static boolean contains(Interval interval, Instant at) {
        return !at.isBefore(interval.start()) && at.isBefore(interval.end());
    }

    /// End of the last scanned day.
    private static Instant horizonEnd(ZoneId zone, Instant at) {
        return at.atZone(zone).toLocalDate().plusDays(HORIZON_DAYS + 1L).atStartOfDay(zone).toInstant();
    }

    /// Open intervals from the day before `at` to the horizon, sorted and with touching intervals merged.
    private static List<Interval> intervalsAround(List<Slot> hours, ZoneId zone, Instant at) {
        if (hours == null || hours.isEmpty()) {
            return List.of();
        }
        var today = at.atZone(zone).toLocalDate();
        var raw = new ArrayList<Interval>();
        for (LocalDate day = today.minusDays(1); !day.isAfter(today.plusDays(HORIZON_DAYS)); day = day.plusDays(1)) {
            for (var slot : hours) {
                if (slot.closed() || slot.day() != day.getDayOfWeek() || slot.opens() == null || slot.closes() == null) {
                    continue;
                }
                var start = LocalDateTime.of(day, slot.opens()).atZone(zone).toInstant();
                var closes = slot.closes().equals(END_OF_DAY) ? day.plusDays(1).atStartOfDay() : LocalDateTime.of(day, slot.closes());
                var end = closes.atZone(zone).toInstant();
                if (end.isAfter(start)) {
                    raw.add(new Interval(start, end));
                }
            }
        }
        raw.sort(Comparator.comparing(Interval::start));
        var merged = new ArrayList<Interval>();
        for (var interval : raw) {
            if (!merged.isEmpty() && !interval.start().isAfter(merged.getLast().end())) {
                var last = merged.removeLast();
                merged.add(new Interval(last.start(), last.end().isAfter(interval.end()) ? last.end() : interval.end()));
            } else {
                merged.add(interval);
            }
        }
        return merged;
    }
}
