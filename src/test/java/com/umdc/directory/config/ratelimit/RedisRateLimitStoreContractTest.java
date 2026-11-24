package com.umdc.directory.config.ratelimit;

class RedisRateLimitStoreContractTest extends RateLimitStoreContractTest {
    @Override
    RateLimitStore create() {
        FakeScriptRedisTemplate template = new FakeScriptRedisTemplate(() -> nanos.get() / 1_000_000L);
        return new RedisRateLimitStore(template, new InMemoryRateLimitStore(nanos::get),
                new RedisFailOpenGuard("t", () -> nanos.get() / 1_000_000L, 5_000));
    }
}
