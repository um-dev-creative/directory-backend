package com.umdc.directory.constant;

import java.util.Locale;
import java.util.Optional;

/// Kind of campaign (RF-03). Stored in `campaign.type` as the enum name.
public enum CampaignType {
    PROMOTION,
    EVENT,
    ANNOUNCEMENT;

    /// Case-insensitive parse; empty when the text is null, blank or not a type.
    public static Optional<CampaignType> parse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(text.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
