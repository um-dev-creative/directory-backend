package com.umdc.directory.client.geocoding;

/// Provider failure with no internal detail (no URL, key or query).
public class GeocodingException extends RuntimeException {

    /// Failure kinds, mapped to `504` and `502`.
    public enum Kind { TIMEOUT, UNAVAILABLE }

    private final transient Kind kind;

    public GeocodingException(Kind kind) {
        super("geocoding provider " + kind.name().toLowerCase(java.util.Locale.ROOT));
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
