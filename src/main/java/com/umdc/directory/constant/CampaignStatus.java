package com.umdc.directory.constant;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/// Lifecycle status of a campaign (RF-03). It is computed, never stored, except {@link #DRAFT}, the only value
/// persisted in `campaign.status`.
///
/// Precedence: DRAFT, then EXPIRED (end date passed), then PAUSED (`active = false`), then SCHEDULED (not started)
/// and finally ACTIVE.
public enum CampaignStatus {
    DRAFT,
    SCHEDULED,
    ACTIVE,
    PAUSED,
    EXPIRED;

    /// Value kept in `campaign.status` for a draft.
    public static final String STORED_DRAFT = "DRAFT";

    /// Computes the status from the stored status, the active flag and the dates. A null active flag counts as active;
    /// a null date is ignored.
    public static CampaignStatus compute(String storedStatus, Boolean active, LocalDateTime start, LocalDateTime end,
                                         LocalDateTime now) {
        if (STORED_DRAFT.equalsIgnoreCase(storedStatus)) {
            return DRAFT;
        }
        if (end != null && end.isBefore(now)) {
            return EXPIRED;
        }
        if (Boolean.FALSE.equals(active)) {
            return PAUSED;
        }
        if (start != null && start.isAfter(now)) {
            return SCHEDULED;
        }
        return ACTIVE;
    }

    /// Case-insensitive parse; empty when the text is null, blank or not a status.
    public static Optional<CampaignStatus> parse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(text.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /// Statuses reachable from this one in one step. EXPIRED is final.
    public Set<CampaignStatus> allowedTargets() {
        return switch (this) {
            case DRAFT -> EnumSet.of(SCHEDULED, ACTIVE);
            case SCHEDULED -> EnumSet.of(ACTIVE, PAUSED, EXPIRED);
            case ACTIVE -> EnumSet.of(PAUSED, EXPIRED);
            case PAUSED -> EnumSet.of(SCHEDULED, ACTIVE, EXPIRED);
            case EXPIRED -> EnumSet.noneOf(CampaignStatus.class);
        };
    }

    public boolean canTransitionTo(CampaignStatus target) {
        return allowedTargets().contains(target);
    }

    /// Consumers (public listing) only see campaigns that are scheduled or running.
    public boolean isPublic() {
        return this == SCHEDULED || this == ACTIVE;
    }
}
