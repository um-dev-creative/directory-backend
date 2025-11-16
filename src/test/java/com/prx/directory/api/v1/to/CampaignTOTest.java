package com.prx.directory.api.v1.to;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CampaignTOTest {

    @Test
    void isStartBeforeOrEqualEnd_nullDates_returnsTrue() {
        CampaignTO t = new CampaignTO(
                UUID.randomUUID(),
                "Campaign",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true
        );

        assertTrue(t.isStartBeforeOrEqualEnd(), "When startDate or endDate is null the validator should return true");
    }

    @Test
    void isStartBeforeOrEqualEnd_startBeforeEnd_returnsTrue() {
        Instant start = Instant.parse("2025-11-01T00:00:00Z");
        Instant end = Instant.parse("2025-11-02T00:00:00Z");

        CampaignTO t = new CampaignTO(
                UUID.randomUUID(),
                "Campaign",
                "desc",
                start,
                end,
                null,
                null,
                null,
                null,
                true
        );

        assertTrue(t.isStartBeforeOrEqualEnd());
    }

    @Test
    void isStartBeforeOrEqualEnd_startEqualsEnd_returnsTrue() {
        Instant instant = Instant.parse("2025-11-01T00:00:00Z");

        CampaignTO t = new CampaignTO(
                UUID.randomUUID(),
                "Campaign",
                "desc",
                instant,
                instant,
                null,
                null,
                null,
                null,
                true
        );

        assertTrue(t.isStartBeforeOrEqualEnd());
    }

    @Test
    void isStartBeforeOrEqualEnd_startAfterEnd_returnsFalse() {
        Instant start = Instant.parse("2025-11-03T00:00:00Z");
        Instant end = Instant.parse("2025-11-02T00:00:00Z");

        CampaignTO t = new CampaignTO(
                UUID.randomUUID(),
                "Campaign",
                "desc",
                start,
                end,
                null,
                null,
                null,
                null,
                true
        );

        assertFalse(t.isStartBeforeOrEqualEnd());
    }
}

