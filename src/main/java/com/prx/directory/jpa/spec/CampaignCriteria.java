package com.prx.directory.jpa.spec;

import java.time.LocalDateTime;

/**
 * Aggregates optional filters for campaign activity and scheduling windows.
 */
public class CampaignCriteria {
    private final Boolean active;
    private final LocalDateTime startFrom;
    private final LocalDateTime startTo;
    private final LocalDateTime endFrom;
    private final LocalDateTime endTo;

    public CampaignCriteria(Boolean active, LocalDateTime startFrom, LocalDateTime startTo, LocalDateTime endFrom, LocalDateTime endTo) {
        this.active = active;
        this.startFrom = startFrom;
        this.startTo = startTo;
        this.endFrom = endFrom;
        this.endTo = endTo;
    }

    public Boolean getActive() {
        return active;
    }

    public LocalDateTime getStartFrom() {
        return startFrom;
    }

    public LocalDateTime getStartTo() {
        return startTo;
    }

    public LocalDateTime getEndFrom() {
        return endFrom;
    }

    public LocalDateTime getEndTo() {
        return endTo;
    }

    public static CampaignCriteria of(Boolean active, LocalDateTime startFrom, LocalDateTime startTo, LocalDateTime endFrom, LocalDateTime endTo) {
        return new CampaignCriteria(active, startFrom, startTo, endFrom, endTo);
    }
}

