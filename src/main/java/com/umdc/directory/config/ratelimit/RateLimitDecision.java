package com.umdc.directory.config.ratelimit;

/// Outcome of a rate limit check.
///
/// @param allowed           whether the request may proceed
/// @param limit             the bucket capacity (requests per minute)
/// @param retryAfterSeconds seconds until a token is available again; 0 when the request was allowed
public record RateLimitDecision(boolean allowed, int limit, long retryAfterSeconds) {
}
