package com.umdc.directory.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serial;
import java.io.Serializable;

/// Number of click events of one type on a business (and optionally a campaign) on one UTC day
/// (`directory_site.business_event_daily`). It holds no personal data: no IP, no user id, no per-click rows.
@Entity
@Table(name = "business_event_daily", schema = "directory_site")
public class BusinessEventDailyEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 5520914488120556977L;

    @EmbeddedId
    private BusinessEventDailyId id;

    @Column(name = "event_count", nullable = false)
    private long eventCount;

    public BusinessEventDailyEntity() {
        // Default constructor
    }

    public BusinessEventDailyEntity(BusinessEventDailyId id, long eventCount) {
        this.id = id;
        this.eventCount = eventCount;
    }

    public BusinessEventDailyId getId() {
        return id;
    }

    public void setId(BusinessEventDailyId id) {
        this.id = id;
    }

    public long getEventCount() {
        return eventCount;
    }

    public void setEventCount(long eventCount) {
        this.eventCount = eventCount;
    }
}
