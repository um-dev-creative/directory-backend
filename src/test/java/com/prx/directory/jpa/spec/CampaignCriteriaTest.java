package com.prx.directory.jpa.spec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignCriteriaTest {

    @Test
    @DisplayName("CampaignCriteria: of factory and getters")
    void ofAndGetters() {
        LocalDateTime now = LocalDateTime.now();
        CampaignCriteria c = CampaignCriteria.of(Boolean.TRUE, now, now.plusSeconds(3600), now.minusSeconds(3600), now);

        assertTrue(c.getActive());
        assertEquals(now, c.getStartFrom());
        assertEquals(now.plusSeconds(3600), c.getStartTo());
        assertEquals(now.minusSeconds(3600), c.getEndFrom());
        assertEquals(now, c.getEndTo());
    }
}
