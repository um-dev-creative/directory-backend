package com.umdc.directory.client.mercury;

/// Mercury asked Directory to slow down (429). Nothing is sent to Mercury until `retryAfterSeconds` have passed.
public class MercuryThrottledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long retryAfterSeconds;

    /// @param retryAfterSeconds seconds to wait, from Mercury's `Retry-After` header
    public MercuryThrottledException(long retryAfterSeconds) {
        super("Mercury is throttling this client; retry in " + retryAfterSeconds + " seconds.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /// @param retryAfterSeconds seconds to wait, from Mercury's `Retry-After` header
    /// @param cause             the 429 answer
    public MercuryThrottledException(long retryAfterSeconds, Throwable cause) {
        super("Mercury is throttling this client; retry in " + retryAfterSeconds + " seconds.", cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /// @return seconds to wait before calling Mercury again
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
