package com.prx.directory.jpa.spec;

import java.time.Instant;

/**
 * Aggregates optional filters for campaign activity and scheduling windows.
 */
public class CampaignCriteria {
    private final Boolean active;
    private final Instant startFrom;
    private final Instant startTo;
    private final Instant endFrom;
    private final Instant endTo;

    public CampaignCriteria(Boolean active, Instant startFrom, Instant startTo, Instant endFrom, Instant endTo) {
        this.active = active;
        this.startFrom = startFrom;
        this.startTo = startTo;
        this.endFrom = endFrom;
        this.endTo = endTo;
    }

    public Boolean getActive() {
        return active;
    }

    public Instant getStartFrom() {
        return startFrom;
    }

    public Instant getStartTo() {
        return startTo;
    }

    public Instant getEndFrom() {
        return endFrom;
    }

    public Instant getEndTo() {
        return endTo;
    }

    public static CampaignCriteria of(Boolean active, Instant startFrom, Instant startTo, Instant endFrom, Instant endTo) {
        return new CampaignCriteria(active, startFrom, startTo, endFrom, endTo);
    }
}

