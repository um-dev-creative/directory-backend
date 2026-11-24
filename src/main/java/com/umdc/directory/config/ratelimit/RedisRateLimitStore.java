package com.umdc.directory.config.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

/// Token bucket per key kept in Redis, shared by all instances. The refill/consume step runs atomically in one Lua script with
/// the same arithmetic as {@link InMemoryRateLimitStore} (refill `permitsPerMinute / 60` tokens per second, capacity
/// `permitsPerMinute`, `Retry-After` = seconds until one token is available, at least 1), so limit, `Retry-After` and
/// `X-RateLimit-Limit` are identical. The clock is Redis `TIME`, so instances with different clocks agree.
///
/// Fails open: when Redis errors, the request is decided by the in-memory `fallback` store and a {@link RedisFailOpenGuard}
/// skips Redis for a few seconds so each request does not wait for the timeout. An outage never produces a 5xx.
public class RedisRateLimitStore implements RateLimitStore {

    /// Prefix of every bucket key in Redis.
    static final String KEY_PREFIX = "umdc:directory:rl:";
    /// Bucket expiry: a bucket needs at most 60 s to refill completely, so an idle one is equivalent to a new one.
    static final long EXPIRE_MILLIS = 120_000L;

    @SuppressWarnings({"rawtypes", "unchecked"})
    static final RedisScript<List> SCRIPT = new DefaultRedisScript<>("""
            local t = redis.call('TIME')
            local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
            local permits = tonumber(ARGV[1])
            local data = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
            local tokens = tonumber(data[1])
            local ts = tonumber(data[2])
            if tokens == nil or ts == nil then
              tokens = permits
              ts = now
            end
            tokens = math.min(permits, tokens + math.max(0, now - ts) * permits / 60000)
            local allowed = 0
            local retry = 0
            if tokens >= 1 then
              tokens = tokens - 1
              allowed = 1
            else
              retry = math.max(1, math.ceil((1 - tokens) * 60 / permits))
            end
            redis.call('HSET', KEYS[1], 'tokens', string.format('%.6f', tokens), 'ts', tostring(now))
            redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[2]))
            return {allowed, retry}
            """, List.class);

    private final StringRedisTemplate template;
    private final RateLimitStore fallback;
    private final RedisFailOpenGuard guard;

    /// @param template Redis access
    /// @param fallback store used while Redis is down (normally an {@link InMemoryRateLimitStore})
    /// @param guard    circuit breaker
    public RedisRateLimitStore(StringRedisTemplate template, RateLimitStore fallback, RedisFailOpenGuard guard) {
        this.template = template;
        this.fallback = fallback;
        this.guard = guard;
    }

    @Override
    public RateLimitDecision tryConsume(String key, int permitsPerMinute) {
        if (!guard.permits()) {
            return fallback.tryConsume(key, permitsPerMinute);
        }
        try {
            List<?> result = template.execute(SCRIPT, List.of(KEY_PREFIX + key), Integer.toString(permitsPerMinute),
                    Long.toString(EXPIRE_MILLIS));
            if (result == null || result.size() < 2) {
                throw new IllegalStateException("Unexpected rate limit script result");
            }
            boolean allowed = ((Number) result.get(0)).longValue() == 1L;
            return new RateLimitDecision(allowed, permitsPerMinute, allowed ? 0 : ((Number) result.get(1)).longValue());
        } catch (RuntimeException e) { // NOPMD - any Redis/driver failure must fail open
            guard.failure(e);
            return fallback.tryConsume(key, permitsPerMinute);
        }
    }
}
