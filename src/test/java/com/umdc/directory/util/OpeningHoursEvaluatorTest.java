package com.umdc.directory.util;

import com.umdc.directory.util.OpeningHoursEvaluator.Slot;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningHoursEvaluatorTest {

    private static final ZoneId TORONTO = ZoneId.of("America/Toronto");

    private static Slot open(DayOfWeek d, String from, String to) {
        return new Slot(d, LocalTime.parse(from), LocalTime.parse(to), false);
    }

    private static Instant at(String local, ZoneId zone) {
        return java.time.LocalDateTime.parse(local).atZone(zone).toInstant();
    }

    /// Monday to Friday 09:00-13:00 and 15:00-21:00.
    private static List<Slot> weekdays() {
        var slots = new ArrayList<Slot>();
        for (var d : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            slots.add(open(d, "09:00", "13:00"));
            slots.add(open(d, "15:00", "21:00"));
        }
        return slots;
    }

    @Test
    void openInsideASlotAndClosedBetweenSlots() {
        // 2026-11-23 is a Monday
        assertTrue(OpeningHoursEvaluator.isOpenNow(weekdays(), TORONTO, at("2026-11-23T10:00:00", TORONTO)));
        assertFalse(OpeningHoursEvaluator.isOpenNow(weekdays(), TORONTO, at("2026-11-23T14:00:00", TORONTO)));
        assertTrue(OpeningHoursEvaluator.isOpenNow(weekdays(), TORONTO, at("2026-11-23T20:59:59", TORONTO)));
    }

    @Test
    void openingIsIncludedAndClosingExcluded() {
        assertTrue(OpeningHoursEvaluator.isOpenNow(weekdays(), TORONTO, at("2026-11-23T09:00:00", TORONTO)));
        assertFalse(OpeningHoursEvaluator.isOpenNow(weekdays(), TORONTO, at("2026-11-23T21:00:00", TORONTO)));
    }

    @Test
    void closedOnDaysWithoutSlotsAndOnClosedDays() {
        assertFalse(OpeningHoursEvaluator.isOpenNow(weekdays(), TORONTO, at("2026-11-22T12:00:00", TORONTO))); // Sunday
        var withClosedSunday = new ArrayList<>(weekdays());
        withClosedSunday.add(new Slot(DayOfWeek.SUNDAY, null, null, true));
        assertFalse(OpeningHoursEvaluator.isOpenNow(withClosedSunday, TORONTO, at("2026-11-22T12:00:00", TORONTO)));
    }

    @Test
    void emptyOrNullHoursAreNeverOpenAndHaveNoChange() {
        var now = at("2026-11-23T10:00:00", TORONTO);
        assertFalse(OpeningHoursEvaluator.isOpenNow(List.of(), TORONTO, now));
        assertFalse(OpeningHoursEvaluator.isOpenNow(null, TORONTO, now));
        assertTrue(OpeningHoursEvaluator.nextChange(null, TORONTO, now).isEmpty());
        assertTrue(OpeningHoursEvaluator.nextChange(List.of(new Slot(DayOfWeek.MONDAY, null, null, true)), TORONTO, now).isEmpty());
    }

    @Test
    void theZoneDecidesTheLocalDayAndHour() {
        // 2026-11-23T01:00Z is still Sunday 20:00 in Toronto (closed) but Monday 10:00 in Tokyo (open)
        var instant = Instant.parse("2026-11-23T01:00:00Z");
        assertFalse(OpeningHoursEvaluator.isOpenNow(weekdays(), TORONTO, instant));
        assertTrue(OpeningHoursEvaluator.isOpenNow(weekdays(), ZoneId.of("Asia/Tokyo"), instant));
    }

    @Test
    void nextChangeWhenOpenIsTheClosingOfTheSlot() {
        var change = OpeningHoursEvaluator.nextChange(weekdays(), TORONTO, at("2026-11-23T20:30:00", TORONTO)).orElseThrow();
        assertFalse(change.opening());
        assertEquals(at("2026-11-23T21:00:00", TORONTO), change.at());
    }

    @Test
    void nextChangeWhenClosedIsTheNextOpening() {
        var between = OpeningHoursEvaluator.nextChange(weekdays(), TORONTO, at("2026-11-23T14:00:00", TORONTO)).orElseThrow();
        assertTrue(between.opening());
        assertEquals(at("2026-11-23T15:00:00", TORONTO), between.at());
        // Friday evening: next opening is Monday 09:00
        var weekend = OpeningHoursEvaluator.nextChange(weekdays(), TORONTO, at("2026-11-27T22:00:00", TORONTO)).orElseThrow();
        assertEquals(at("2026-11-30T09:00:00", TORONTO), weekend.at());
    }

    @Test
    void aSlotUntil2359JoinsTheNextDaySlotFromMidnight() {
        var slots = List.of(open(DayOfWeek.FRIDAY, "18:00", "23:59"), open(DayOfWeek.SATURDAY, "00:00", "02:00"));
        // 2026-11-27 is a Friday
        assertTrue(OpeningHoursEvaluator.isOpenNow(slots, TORONTO, at("2026-11-27T23:59:30", TORONTO)));
        assertTrue(OpeningHoursEvaluator.isOpenNow(slots, TORONTO, at("2026-11-28T01:00:00", TORONTO)));
        var change = OpeningHoursEvaluator.nextChange(slots, TORONTO, at("2026-11-27T19:00:00", TORONTO)).orElseThrow();
        assertFalse(change.opening());
        assertEquals(at("2026-11-28T02:00:00", TORONTO), change.at());
    }

    @Test
    void anAlwaysOpenScheduleHasNoForeseeableChange() {
        var slots = new ArrayList<Slot>();
        for (var d : DayOfWeek.values()) {
            slots.add(open(d, "00:00", "23:59"));
        }
        var now = at("2026-11-23T10:00:00", TORONTO);
        assertTrue(OpeningHoursEvaluator.isOpenNow(slots, TORONTO, now));
        assertTrue(OpeningHoursEvaluator.nextChange(slots, TORONTO, now).isEmpty());
    }

    @Test
    void anOverlappingSlotExtendsTheOpening() {
        var slots = List.of(open(DayOfWeek.MONDAY, "09:00", "12:00"), open(DayOfWeek.MONDAY, "11:00", "14:00"));
        var change = OpeningHoursEvaluator.nextChange(slots, TORONTO, at("2026-11-23T10:00:00", TORONTO)).orElseThrow();
        assertEquals(at("2026-11-23T14:00:00", TORONTO), change.at());
    }

    @Test
    void slotsWithMissingTimesAreIgnored() {
        var slots = List.of(new Slot(DayOfWeek.MONDAY, null, LocalTime.NOON, false), open(DayOfWeek.MONDAY, "10:00", "11:00"));
        assertFalse(OpeningHoursEvaluator.isOpenNow(slots, TORONTO, at("2026-11-23T09:00:00", TORONTO)));
        assertTrue(OpeningHoursEvaluator.isOpenNow(slots, TORONTO, at("2026-11-23T10:30:00", TORONTO)));
    }

    @Test
    void springForwardKeepsTheLocalHoursAndTheRealDuration() {
        // DST starts in Toronto on 2027-03-14 (Sunday): 02:00 jumps to 03:00. Open Sunday 00:00-04:00.
        var slots = List.of(open(DayOfWeek.SUNDAY, "00:00", "04:00"));
        var before = Instant.parse("2027-03-14T06:30:00Z"); // 01:30 EST
        var change = OpeningHoursEvaluator.nextChange(slots, TORONTO, before).orElseThrow();
        assertEquals(Instant.parse("2027-03-14T08:00:00Z"), change.at()); // 04:00 EDT = 08:00Z, only 1.5 h after
        assertTrue(OpeningHoursEvaluator.isOpenNow(slots, TORONTO, Instant.parse("2027-03-14T07:30:00Z"))); // 03:30 EDT
        assertFalse(OpeningHoursEvaluator.isOpenNow(slots, TORONTO, Instant.parse("2027-03-14T08:00:00Z")));
    }

    @Test
    void aSlotInsideTheSkippedHourStartsWhenTheClockResumes() {
        var slots = List.of(open(DayOfWeek.SUNDAY, "02:30", "05:00"));
        var change = OpeningHoursEvaluator.nextChange(slots, TORONTO, Instant.parse("2027-03-14T05:00:00Z")).orElseThrow();
        assertTrue(change.opening());
        assertEquals(Instant.parse("2027-03-14T07:30:00Z"), change.at()); // 02:30 does not exist; resolved to 03:30 EDT
    }

    @Test
    void fallBackRepeatsTheHourWithoutBreakingTheClosing() {
        // DST ends in Toronto on 2026-11-01 (Sunday): 02:00 EDT goes back to 01:00 EST. Open Sunday 00:00-03:00.
        var slots = List.of(open(DayOfWeek.SUNDAY, "00:00", "03:00"));
        var first = Instant.parse("2026-11-01T05:30:00Z");  // 01:30 EDT
        var second = Instant.parse("2026-11-01T06:30:00Z"); // 01:30 EST
        assertTrue(OpeningHoursEvaluator.isOpenNow(slots, TORONTO, first));
        assertTrue(OpeningHoursEvaluator.isOpenNow(slots, TORONTO, second));
        assertEquals(Instant.parse("2026-11-01T08:00:00Z"), OpeningHoursEvaluator.nextChange(slots, TORONTO, first).orElseThrow().at()); // 03:00 EST
    }
}
