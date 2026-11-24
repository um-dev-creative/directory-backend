package com.umdc.directory.config.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisRateLimitStoreTest {

    private final AtomicLong millis = new AtomicLong(1_000_000);
    private FakeScriptRedisTemplate template;
    private RedisRateLimitStore store;

    @BeforeEach
    void setUp() {
        template = new FakeScriptRedisTemplate(millis::get);
        store = new RedisRateLimitStore(template, new InMemoryRateLimitStore(() -> millis.get() * 1_000_000L),
                new RedisFailOpenGuard("t", millis::get, 5_000));
    }

    @Test
    void usesRedisWhenAvailable() {
        assertThat(store.tryConsume("k", 1).allowed()).isTrue();
        assertThat(store.tryConsume("k", 1).allowed()).isFalse();
        assertThat(template.calls).isEqualTo(2);
    }

    @Test
    void failsOpenToTheLocalStoreAndKeepsLimiting() {
        template.down = true;
        assertThat(store.tryConsume("k", 1).allowed()).isTrue();
        RateLimitDecision d = store.tryConsume("k", 1);
        assertThat(d.allowed()).isFalse();
        assertThat(d.limit()).isEqualTo(1);
        assertThat(d.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    void breakerSkipsRedisWhileOpenAndProbesAgainAfterTheWindow() {
        template.down = true;
        store.tryConsume("k", 5);
        assertThat(template.calls).isEqualTo(1);
        store.tryConsume("k", 5);
        store.tryConsume("k", 5);
        assertThat(template.calls).isEqualTo(1); // skipped, no timeout paid
        millis.addAndGet(5_000);
        template.down = false;
        store.tryConsume("k", 5);
        assertThat(template.calls).isEqualTo(2);
        store.tryConsume("k", 5);
        assertThat(template.calls).isEqualTo(3); // closed again
    }

    @Test
    void aFailedProbeReopensTheBreaker() {
        template.down = true;
        store.tryConsume("k", 5);
        millis.addAndGet(5_000);
        store.tryConsume("k", 5);
        store.tryConsume("k", 5);
        assertThat(template.calls).isEqualTo(2);
    }

    @Test
    void anEmptyOrMalformedScriptResultFailsOpen() {
        template.nextResult = FakeScriptRedisTemplate.NULL;
        assertThat(store.tryConsume("a", 5).allowed()).isTrue();
        millis.addAndGet(5_000);
        template.nextResult = List.of(1L);
        assertThat(store.tryConsume("b", 5).allowed()).isTrue();
    }

    @Test
    void theConfigDeclaresARedisStoreAndIsOnlyActiveForTheRedisValue() {
        assertThat(new RedisRateLimitConfig().redisRateLimitStore(mock(StringRedisTemplate.class))).isInstanceOf(RedisRateLimitStore.class);
        ConditionalOnProperty condition = RedisRateLimitConfig.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(condition.name()).containsExactly("umdc.ratelimit.store");
        assertThat(condition.havingValue()).isEqualTo("redis");
    }

    @Test
    void theDefaultGuardUsesTheSystemClock() {
        RedisFailOpenGuard guard = new RedisFailOpenGuard("d");
        assertThat(guard.permits()).isTrue();
        guard.failure(new IllegalStateException("x"));
        assertThat(guard.permits()).isFalse();
    }
}
