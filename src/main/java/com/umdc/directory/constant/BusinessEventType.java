package com.umdc.directory.constant;

import java.util.Locale;
import java.util.Optional;

/// Kinds of click events a consumer can report on a business (RF-10).
public enum BusinessEventType {
    ROUTE, CALL, WEB, SAVE;

    /// @param value the text sent by the client (case-insensitive)
    /// @return the type, or empty when the text is null, blank or unknown
    public static Optional<BusinessEventType> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
