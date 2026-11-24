package com.umdc.directory.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/// Key of the daily event counter: business, campaign (the nil UUID when the event is not tied to a campaign), type and UTC day.
@Embeddable
public class BusinessEventDailyId implements Serializable {
    @Serial
    private static final long serialVersionUID = 7410022918342210054L;

    /// Stored in `campaign_id` when the event has no campaign (a primary key column cannot be null).
    public static final UUID NO_CAMPAIGN = new UUID(0L, 0L);

    @NotNull
    @Column(name = "business_fk", nullable = false)
    private UUID businessId;

    @NotNull
    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @NotNull
    @Column(name = "event_type", nullable = false, length = 16)
    private String eventType;

    @NotNull
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    public BusinessEventDailyId() {
        // Default constructor
    }

    public BusinessEventDailyId(UUID businessId, UUID campaignId, String eventType, LocalDate eventDate) {
        this.businessId = businessId;
        this.campaignId = campaignId;
        this.eventType = eventType;
        this.eventDate = eventDate;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BusinessEventDailyId that = (BusinessEventDailyId) o;
        return Objects.equals(businessId, that.businessId) && Objects.equals(campaignId, that.campaignId)
                && Objects.equals(eventType, that.eventType) && Objects.equals(eventDate, that.eventDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessId, campaignId, eventType, eventDate);
    }
}
