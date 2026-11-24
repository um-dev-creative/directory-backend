package com.umdc.directory.config.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/// Short circuit breaker shared by the Redis rate limit store and the geo cache. After a Redis failure it stays open for
/// `openMillis`: during that time callers skip Redis (and use their fallback) instead of waiting for the connection timeout on
/// every request. When the window ends the next call probes Redis again; a failure re-opens it, a success keeps it closed.
public class RedisFailOpenGuard {

    /// Default length of the open window.
    public static final long DEFAULT_OPEN_MILLIS = 5_000L;

    private static final Logger logger = LoggerFactory.getLogger(RedisFailOpenGuard.class);

    private final LongSupplier millisClock;
    private final long openMillis;
    private final String name;
    private final AtomicLong openUntil = new AtomicLong();

    /// @param name       label used in the warning
    /// @param millisClock time source in milliseconds (injectable for tests)
    /// @param openMillis how long Redis is skipped after a failure
    public RedisFailOpenGuard(String name, LongSupplier millisClock, long openMillis) {
        this.name = name;
        this.millisClock = millisClock;
        this.openMillis = Math.max(1, openMillis);
    }

    /// Guard using `System.currentTimeMillis` and {@link #DEFAULT_OPEN_MILLIS}.
    public RedisFailOpenGuard(String name) {
        this(name, System::currentTimeMillis, DEFAULT_OPEN_MILLIS);
    }

    /// @return `true` when Redis may be called (breaker closed or probing)
    public boolean permits() {
        return millisClock.getAsLong() >= openUntil.get();
    }

    /// Records a Redis failure: opens the breaker and logs a warning (without the exception stack: it may echo the address).
    ///
    /// @param e the failure
    public void failure(RuntimeException e) {
        openUntil.set(millisClock.getAsLong() + openMillis);
        logger.warn("{} Redis unavailable, failing open for {} ms: {}", name, openMillis, e.getClass().getSimpleName());
    }
}
