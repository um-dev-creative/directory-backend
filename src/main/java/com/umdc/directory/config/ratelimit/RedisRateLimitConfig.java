package com.umdc.directory.config.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/// Declares the Redis {@link RateLimitStore} when `umdc.ratelimit.store=redis` (env `UMDC_RATELIMIT_STORE`; default `memory`,
/// which declares nothing, so {@link RateLimitConfig} keeps using {@link InMemoryRateLimitStore}).
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "umdc.ratelimit.store", havingValue = "redis")
public class RedisRateLimitConfig {

    /// @param template Redis access (connection settings: `REDIS_URL` and friends)
    /// @return the Redis store, failing open to a local in-memory store
    @Bean
    public RateLimitStore redisRateLimitStore(StringRedisTemplate template) {
        return new RedisRateLimitStore(template, new InMemoryRateLimitStore(), new RedisFailOpenGuard("rate-limit"));
    }
}
