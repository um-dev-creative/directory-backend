package com.prx.directory.jpa.spec;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CampaignCriteriaTest {

    @Test
    void ofAndGetters() {
        Instant now = Instant.now();
        CampaignCriteria c = CampaignCriteria.of(Boolean.TRUE, now, now.plusSeconds(3600), now.minusSeconds(3600), now);

        assertTrue(c.getActive());
        assertEquals(now, c.getStartFrom());
        assertEquals(now.plusSeconds(3600), c.getStartTo());
        assertEquals(now.minusSeconds(3600), c.getEndFrom());
        assertEquals(now, c.getEndTo());
    }
}

