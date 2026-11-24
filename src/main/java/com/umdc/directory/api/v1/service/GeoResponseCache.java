package com.umdc.directory.api.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umdc.directory.config.ratelimit.RedisFailOpenGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/// Redis cache of the anonymous results of `GET /businesses/map` and `/nearby`. Off by default
/// (`umdc.geo.cache-enabled=false`); entries expire only by TTL (`umdc.geo.cache-ttl-seconds`, default 60). It never holds
/// per-user data (the keys are built from the query only) and it fails open: any Redis problem is a cache miss, never an
/// error, and a {@link RedisFailOpenGuard} avoids waiting for the timeout on every request.
@Component
public class GeoResponseCache {

    private static final Logger logger = LoggerFactory.getLogger(GeoResponseCache.class);
    /// Entries larger than this many bytes are not cached.
    static final int MAX_VALUE_BYTES = 512 * 1024;

    private final StringRedisTemplate template;
    private final boolean enabled;
    private final Duration ttl;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RedisFailOpenGuard guard;

    /// @param templateProvider Redis access (may be absent)
    /// @param enabled          `umdc.geo.cache-enabled` (default false)
    /// @param ttlSeconds       `umdc.geo.cache-ttl-seconds` (default 60)
    @Autowired
    public GeoResponseCache(ObjectProvider<StringRedisTemplate> templateProvider,
                            @Value("${umdc.geo.cache-enabled:false}") boolean enabled,
                            @Value("${umdc.geo.cache-ttl-seconds:60}") long ttlSeconds) {
        this(templateProvider.getIfAvailable(), enabled, ttlSeconds, new RedisFailOpenGuard("geo-cache"));
    }

    GeoResponseCache(StringRedisTemplate template, boolean enabled, long ttlSeconds, RedisFailOpenGuard guard) {
        this.template = template;
        this.enabled = enabled && template != null;
        this.ttl = Duration.ofSeconds(Math.max(1, ttlSeconds));
        this.guard = guard;
    }

    /// @return a cache that never stores anything
    public static GeoResponseCache disabled() {
        return new GeoResponseCache(null, false, 1, new RedisFailOpenGuard("geo-cache"));
    }

    /// @return `true` when results are read from and written to Redis
    public boolean enabled() {
        return enabled;
    }

    /// @param key  cache key
    /// @param type result type
    /// @param <T>  result type
    /// @return the cached value, empty on miss, disabled cache, breaker open or any Redis/JSON failure
    public <T> Optional<T> get(String key, Class<T> type) {
        if (!enabled || !guard.permits()) {
            return Optional.empty();
        }
        try {
            String json = template.opsForValue().get(key);
            return json == null ? Optional.empty() : Optional.of(mapper.readValue(json, type));
        } catch (RuntimeException e) { // NOPMD - fail open
            guard.failure(e);
            return Optional.empty();
        } catch (java.io.IOException e) {
            logger.warn("geo-cache entry unreadable, treated as a miss: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /// Stores a value for the configured TTL; silently skipped when disabled, oversized or Redis fails.
    ///
    /// @param key   cache key
    /// @param value value to serialise
    public void put(String key, Object value) {
        if (!enabled || !guard.permits()) {
            return;
        }
        try {
            String json = mapper.writeValueAsString(value);
            if (json.getBytes(StandardCharsets.UTF_8).length > MAX_VALUE_BYTES) {
                return;
            }
            template.opsForValue().set(key, json, ttl);
        } catch (RuntimeException e) { // NOPMD - fail open
            guard.failure(e);
        } catch (java.io.IOException e) {
            logger.warn("geo-cache value not serialisable, skipped: {}", e.getClass().getSimpleName());
        }
    }
}
