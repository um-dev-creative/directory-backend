package com.umdc.directory.config.ratelimit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/// Token bucket per key, kept in this JVM's memory. Buckets refill continuously at `permitsPerMinute / 60` tokens per second.
/// Idle buckets (full again) are evicted periodically so the map does not grow without bound.
public class InMemoryRateLimitStore implements RateLimitStore {

    private static final double NANOS_PER_MINUTE = 60_000_000_000.0;
    private static final int EVICT_EVERY = 1_000;
    /// Default cap of live buckets.
    static final int DEFAULT_MAX_BUCKETS = 100_000;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final LongSupplier nanoClock;
    private final AtomicLong calls = new AtomicLong();
    private final int maxBuckets;

    /// Store using `System.nanoTime`.
    public InMemoryRateLimitStore() {
        this(System::nanoTime);
    }

    /// @param nanoClock monotonic time source in nanoseconds (injectable for tests)
    public InMemoryRateLimitStore(LongSupplier nanoClock) {
        this(nanoClock, DEFAULT_MAX_BUCKETS);
    }

    /// @param nanoClock  monotonic time source in nanoseconds (injectable for tests)
    /// @param maxBuckets hard cap of live buckets; when reached, idle buckets are evicted first and, if still full, the
    ///                   map is trimmed so a flood of distinct keys cannot exhaust memory
    public InMemoryRateLimitStore(LongSupplier nanoClock, int maxBuckets) {
        this.nanoClock = nanoClock;
        this.maxBuckets = Math.max(1, maxBuckets);
    }

    @Override
    public RateLimitDecision tryConsume(String key, int permitsPerMinute) {
        if (calls.incrementAndGet() % EVICT_EVERY == 0) {
            evictIdle();
        }
        if (buckets.size() >= maxBuckets && !buckets.containsKey(key)) {
            makeRoom();
        }
        long now = nanoClock.getAsLong();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(permitsPerMinute, now));
        return bucket.tryConsume(permitsPerMinute, now);
    }

    /// Frees space under the cap: idle buckets first, then arbitrary ones (an evicted active bucket only gets a fresh quota).
    private void makeRoom() {
        evictIdle();
        var it = buckets.keySet().iterator();
        while (buckets.size() >= maxBuckets && it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    /// @return number of live buckets (for tests and diagnostics)
    int size() {
        return buckets.size();
    }

    /// Removes the buckets that have refilled completely.
    void evictIdle() {
        long now = nanoClock.getAsLong();
        buckets.entrySet().removeIf(e -> e.getValue().isIdle(now));
    }

    private static final class Bucket {
        private double tokens;
        private long lastRefill;
        private int capacity;

        Bucket(int capacity, long now) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefill = now;
        }

        synchronized RateLimitDecision tryConsume(int permitsPerMinute, long now) {
            capacity = permitsPerMinute;
            tokens = Math.min(capacity, tokens + Math.max(0, now - lastRefill) * permitsPerMinute / NANOS_PER_MINUTE);
            lastRefill = now;
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return new RateLimitDecision(true, capacity, 0);
            }
            long seconds = (long) Math.ceil((1.0 - tokens) * 60.0 / permitsPerMinute);
            return new RateLimitDecision(false, capacity, Math.max(1, seconds));
        }

        synchronized boolean isIdle(long now) {
            return tokens + Math.max(0, now - lastRefill) * capacity / NANOS_PER_MINUTE >= capacity;
        }
    }
}
