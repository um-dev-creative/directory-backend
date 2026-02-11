package com.prx.directory.api.v1.to;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignTOTest {

    @Test
    @DisplayName("CampaignTO: null dates considered valid for start<=end")
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
    @DisplayName("CampaignTO: start before end returns true")
    void isStartBeforeOrEqualEnd_startBeforeEnd_returnsTrue() {
        LocalDateTime start = LocalDateTime.parse("2025-11-01T00:00:00");
        LocalDateTime end = LocalDateTime.parse("2025-11-02T00:00:00");

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
    @DisplayName("CampaignTO: start equals end returns true")
    void isStartBeforeOrEqualEnd_startEqualsEnd_returnsTrue() {
        LocalDateTime localDateTime = LocalDateTime.parse("2025-11-01T00:00:00");

        CampaignTO t = new CampaignTO(
                UUID.randomUUID(),
                "Campaign",
                "desc",
                localDateTime,
                localDateTime,
                null,
                null,
                null,
                null,
                true
        );

        assertTrue(t.isStartBeforeOrEqualEnd());
    }

    @Test
    @DisplayName("CampaignTO: start after end returns false")
    void isStartBeforeOrEqualEnd_startAfterEnd_returnsFalse() {
        LocalDateTime start = LocalDateTime.parse("2025-11-03T00:00:00");
        LocalDateTime end = LocalDateTime.parse("2025-11-02T00:00:00");

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
