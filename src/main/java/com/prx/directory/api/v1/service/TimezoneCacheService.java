package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.TimezoneResumeTO;

import java.util.List;

/**
 * TimezoneCacheService provides a Redis-backed caching layer for the timezone collection.
 * It encapsulates cache read/write, refresh, and invalidation operations.
 */
public interface TimezoneCacheService {

    /**
     * Returns the full list of timezones, served from Redis cache when available.
     * On a cache miss the list is loaded from PostgreSQL, written to Redis, and returned.
     *
     * @return list of {@link TimezoneResumeTO} objects
     */
    List<TimezoneResumeTO> getAllTimezones();

    /**
     * Forces a reload of the timezone list from PostgreSQL and writes the fresh value to Redis.
     */
    void refreshTimezones();

    /**
     * Deletes the timezone list cache key from Redis.
     * The next call to {@link #getAllTimezones()} will repopulate the cache.
     */
    void invalidateTimezones();
}

