package com.umdc.directory.config.ratelimit;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimitStoreTest {

    private static final long SECOND = 1_000_000_000L;

    @Test
    void allowsUpToTheCapacityThenRejectsWithRetryAfter() {
        InMemoryRateLimitStore store = new InMemoryRateLimitStore(new AtomicLong()::get);
        for (int i = 0; i < 3; i++) {
            assertThat(store.tryConsume("k", 3).allowed()).isTrue();
        }
        RateLimitDecision rejected = store.tryConsume("k", 3);
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.limit()).isEqualTo(3);
        assertThat(rejected.retryAfterSeconds()).isEqualTo(20); // 3 per minute: one token every 20 s
    }

    @Test
    void refillsOverTime() {
        AtomicLong clock = new AtomicLong();
        InMemoryRateLimitStore store = new InMemoryRateLimitStore(clock::get);
        store.tryConsume("k", 60);
        for (int i = 0; i < 59; i++) {
            store.tryConsume("k", 60);
        }
        assertThat(store.tryConsume("k", 60).allowed()).isFalse();
        clock.addAndGet(2 * SECOND);
        assertThat(store.tryConsume("k", 60).allowed()).isTrue();
        assertThat(store.tryConsume("k", 60).allowed()).isTrue();
        assertThat(store.tryConsume("k", 60).allowed()).isFalse();
    }

    @Test
    void keysAreIndependent() {
        InMemoryRateLimitStore store = new InMemoryRateLimitStore(new AtomicLong()::get);
        assertThat(store.tryConsume("a", 1).allowed()).isTrue();
        assertThat(store.tryConsume("a", 1).allowed()).isFalse();
        assertThat(store.tryConsume("b", 1).allowed()).isTrue();
    }

    @Test
    void evictsIdleBuckets() {
        AtomicLong clock = new AtomicLong();
        InMemoryRateLimitStore store = new InMemoryRateLimitStore(clock::get);
        store.tryConsume("a", 2);
        store.tryConsume("b", 2);
        store.evictIdle();
        assertThat(store.size()).isEqualTo(2); // not refilled yet
        clock.addAndGet(120 * SECOND);
        store.evictIdle();
        assertThat(store.size()).isZero();
    }

    @Test
    void evictionRunsPeriodicallyAndDefaultClockWorks() {
        InMemoryRateLimitStore store = new InMemoryRateLimitStore();
        for (int i = 0; i < 1_000; i++) {
            store.tryConsume("k" + i, 1);
        }
        assertThat(store.size()).isPositive();
    }
}
