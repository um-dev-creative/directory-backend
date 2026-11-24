package com.umdc.directory.config.ratelimit;

class InMemoryRateLimitStoreContractTest extends RateLimitStoreContractTest {
    @Override
    RateLimitStore create() {
        return new InMemoryRateLimitStore(nanos::get);
    }
}
