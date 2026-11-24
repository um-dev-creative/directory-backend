package com.umdc.directory.config.ratelimit;

import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/// Test double of Redis for {@link RedisRateLimitStore}: runs, in Java, exactly the arithmetic of the Lua script (same
/// formulas, tokens stored with 6 decimals like `string.format('%.6f')`, expiry honoured) against a controllable clock.
class FakeScriptRedisTemplate extends StringRedisTemplate {

    private final LongSupplier millis;
    private final Map<String, double[]> buckets = new HashMap<>(); // key -> {tokens, ts, expiresAt}
    int calls;
    boolean down;
    Object nextResult;

    FakeScriptRedisTemplate(LongSupplier millis) {
        this.millis = millis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
        calls++;
        if (down) {
            throw new QueryTimeoutException("redis down");
        }
        if (nextResult != null) {
            Object r = nextResult;
            nextResult = null;
            return r == NULL ? null : (T) r;
        }
        double permits = Double.parseDouble((String) args[0]);
        long expire = Long.parseLong((String) args[1]);
        long now = millis.getAsLong();
        double[] b = buckets.get(keys.get(0));
        if (b != null && b[2] <= now) {
            b = null;
        }
        double tokens = b == null ? permits : b[0];
        double ts = b == null ? now : b[1];
        tokens = Math.min(permits, tokens + Math.max(0, now - ts) * permits / 60000);
        long allowed = 0;
        long retry = 0;
        if (tokens >= 1) {
            tokens -= 1;
            allowed = 1;
        } else {
            retry = Math.max(1, (long) Math.ceil((1 - tokens) * 60 / permits));
        }
        buckets.put(keys.get(0), new double[]{Math.round(tokens * 1e6) / 1e6, now, now + expire});
        return (T) List.of(allowed, retry);
    }

    static final Object NULL = new Object();
}
