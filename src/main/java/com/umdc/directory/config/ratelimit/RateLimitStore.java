package com.umdc.directory.config.ratelimit;

/// Storage of the rate limit buckets. The default implementation is in memory ({@link InMemoryRateLimitStore}); a Redis-backed
/// one can replace it later by declaring another bean of this type, with no change in the filter.
public interface RateLimitStore {

    /// Takes one token from the bucket of `key`, creating it with `permitsPerMinute` when it does not exist.
    ///
    /// @param key              bucket identity, for example `ip:10.0.0.1` or `uid:42`
    /// @param permitsPerMinute capacity and refill rate of the bucket (tokens per minute)
    /// @return the decision, with the `Retry-After` hint when rejected
    RateLimitDecision tryConsume(String key, int permitsPerMinute);
}
