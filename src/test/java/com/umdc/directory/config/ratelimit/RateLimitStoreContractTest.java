package com.umdc.directory.config.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/// Behaviour every {@link RateLimitStore} must have: the Redis store is run against the same cases as the in-memory one.
abstract class RateLimitStoreContractTest {

    static final long SECOND = 1_000_000_000L;
    protected final AtomicLong nanos = new AtomicLong(5 * 60 * SECOND);
    protected RateLimitStore store;

    abstract RateLimitStore create();

    @BeforeEach
    void setUp() {
        store = create();
    }

    @Test
    void allowsUpToTheCapacityThenRejectsWithRetryAfter() {
        for (int i = 0; i < 3; i++) {
            RateLimitDecision d = store.tryConsume("k", 3);
            assertThat(d.allowed()).isTrue();
            assertThat(d.limit()).isEqualTo(3);
            assertThat(d.retryAfterSeconds()).isZero();
        }
        RateLimitDecision rejected = store.tryConsume("k", 3);
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.limit()).isEqualTo(3);
        assertThat(rejected.retryAfterSeconds()).isEqualTo(20);
    }

    @Test
    void refillsOverTime() {
        for (int i = 0; i < 60; i++) {
            assertThat(store.tryConsume("k", 60).allowed()).isTrue();
        }
        assertThat(store.tryConsume("k", 60).allowed()).isFalse();
        nanos.addAndGet(2 * SECOND);
        assertThat(store.tryConsume("k", 60).allowed()).isTrue();
        assertThat(store.tryConsume("k", 60).allowed()).isTrue();
        assertThat(store.tryConsume("k", 60).allowed()).isFalse();
    }

    @Test
    void keysAreIndependent() {
        assertThat(store.tryConsume("a", 1).allowed()).isTrue();
        assertThat(store.tryConsume("a", 1).allowed()).isFalse();
        assertThat(store.tryConsume("b", 1).allowed()).isTrue();
    }

    @Test
    void retryAfterIsAtLeastOneSecond() {
        store.tryConsume("k", 600);
        for (int i = 0; i < 599; i++) {
            store.tryConsume("k", 600);
        }
        RateLimitDecision d = store.tryConsume("k", 600);
        assertThat(d.allowed()).isFalse();
        assertThat(d.retryAfterSeconds()).isEqualTo(1);
    }

    @Test
    void anIdleBucketRefillsCompletelyAfterAMinute() {
        store.tryConsume("k", 2);
        store.tryConsume("k", 2);
        assertThat(store.tryConsume("k", 2).allowed()).isFalse();
        nanos.addAndGet(300 * SECOND);
        assertThat(store.tryConsume("k", 2).allowed()).isTrue();
        assertThat(store.tryConsume("k", 2).allowed()).isTrue();
        assertThat(store.tryConsume("k", 2).allowed()).isFalse();
    }
}
