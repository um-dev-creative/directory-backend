package com.umdc.directory.constant;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class CampaignStatusTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 15, 12, 0);
    private static final LocalDateTime PAST = NOW.minusDays(5);
    private static final LocalDateTime FUTURE = NOW.plusDays(5);

    @Test
    void draftWinsOverEverything() {
        assertEquals(CampaignStatus.DRAFT, CampaignStatus.compute("draft", true, PAST, PAST.plusDays(1), NOW));
    }

    @Test
    void expiredWinsOverPaused() {
        assertEquals(CampaignStatus.EXPIRED, CampaignStatus.compute(null, false, PAST, PAST.plusDays(1), NOW));
        assertEquals(CampaignStatus.EXPIRED, CampaignStatus.compute(null, true, PAST, PAST.plusDays(1), NOW));
    }

    @Test
    void inactiveRunningIsPaused() {
        assertEquals(CampaignStatus.PAUSED, CampaignStatus.compute(null, false, PAST, FUTURE, NOW));
    }

    @Test
    void notStartedIsScheduled() {
        assertEquals(CampaignStatus.SCHEDULED, CampaignStatus.compute(null, true, FUTURE, FUTURE.plusDays(1), NOW));
    }

    @Test
    void runningIsActiveAndNullsAreIgnored() {
        assertEquals(CampaignStatus.ACTIVE, CampaignStatus.compute("legacy", true, PAST, FUTURE, NOW));
        assertEquals(CampaignStatus.ACTIVE, CampaignStatus.compute(null, null, null, null, NOW));
    }

    @Test
    void parseIsCaseInsensitive() {
        assertEquals(CampaignStatus.PAUSED, CampaignStatus.parse(" paused ").orElseThrow());
        assertTrue(CampaignStatus.parse("nope").isEmpty());
        assertTrue(CampaignStatus.parse(" ").isEmpty());
        assertTrue(CampaignStatus.parse(null).isEmpty());
    }

    @Test
    void transitionTable() {
        assertEquals(EnumSet.of(CampaignStatus.SCHEDULED, CampaignStatus.ACTIVE), CampaignStatus.DRAFT.allowedTargets());
        assertEquals(EnumSet.of(CampaignStatus.ACTIVE, CampaignStatus.PAUSED, CampaignStatus.EXPIRED), CampaignStatus.SCHEDULED.allowedTargets());
        assertEquals(EnumSet.of(CampaignStatus.PAUSED, CampaignStatus.EXPIRED), CampaignStatus.ACTIVE.allowedTargets());
        assertEquals(EnumSet.of(CampaignStatus.SCHEDULED, CampaignStatus.ACTIVE, CampaignStatus.EXPIRED), CampaignStatus.PAUSED.allowedTargets());
        assertTrue(CampaignStatus.EXPIRED.allowedTargets().isEmpty());
        assertTrue(CampaignStatus.ACTIVE.canTransitionTo(CampaignStatus.PAUSED));
        assertFalse(CampaignStatus.EXPIRED.canTransitionTo(CampaignStatus.ACTIVE));
        assertFalse(CampaignStatus.ACTIVE.canTransitionTo(CampaignStatus.ACTIVE));
    }

    @Test
    void onlyScheduledAndActiveArePublic() {
        for (CampaignStatus s : CampaignStatus.values()) {
            assertEquals(s == CampaignStatus.SCHEDULED || s == CampaignStatus.ACTIVE, s.isPublic(), s.name());
        }
    }

    @Test
    void campaignTypeParse() {
        assertEquals(CampaignType.EVENT, CampaignType.parse(" event ").orElseThrow());
        assertTrue(CampaignType.parse("On-line").isEmpty());
        assertTrue(CampaignType.parse("").isEmpty());
        assertTrue(CampaignType.parse(null).isEmpty());
        assertEquals(3, CampaignType.values().length);
    }
}
