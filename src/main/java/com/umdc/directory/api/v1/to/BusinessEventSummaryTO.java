package com.umdc.directory.api.v1.to;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/// Aggregated click events of a business between two UTC days. There is no personal data in it.
///
/// @param businessId  the business
/// @param from        first day
/// @param to          last day
/// @param total       events in the range
/// @param totalsByType events per type (all four types, zeros included)
/// @param campaigns   one entry per campaign with events; the entry with a null `campaign_id` groups events with no campaign
/// @param daily       one entry per day from `from` to `to`, zeros included, oldest first
public record BusinessEventSummaryTO(
        UUID businessId,
        LocalDate from,
        LocalDate to,
        long total,
        Map<String, Long> totalsByType,
        List<CampaignEvents> campaigns,
        List<DayEvents> daily) {

    /// Events of one campaign.
    public record CampaignEvents(@JsonProperty("campaign_id") UUID campaignId, long total, Map<String, Long> byType) {
    }

    /// Events of one day.
    public record DayEvents(LocalDate date, long total, Map<String, Long> byType) {
    }
}
