package com.umdc.directory.client.mercury;

/// Mercury cannot be used right now: not configured, credentials refused, it failed or it timed out. Maps to 503.
public class MercuryUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /// @param message what happened (never a credential or a token)
    public MercuryUnavailableException(String message) {
        super(message);
    }

    /// @param message what happened (never a credential or a token)
    /// @param cause   the failure
    public MercuryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
