package com.umdc.directory.api.v1.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umdc.directory.api.v1.to.TimezoneResumeTO;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.jpa.repository.TimezoneRepository;
import com.umdc.directory.mapper.TimezoneMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed implementation of {@link TimezoneCacheService}.
 *
 * <p>Cache strategy:
 * <ol>
 *   <li>Read JSON from Redis key {@code timezones:all:v1}.</li>
 *   <li>On hit: deserialize and return.</li>
 *   <li>On miss: acquire a short-lived Redis lock, load from PostgreSQL,
 *       write to Redis, release lock, and return.</li>
 *   <li>If the lock cannot be acquired another instance is already populating
 *       the cache; fall back to a direct DB read.</li>
 * </ol>
 */
@Service
public class TimezoneCacheServiceImpl implements TimezoneCacheService {

    private static final Logger log = LoggerFactory.getLogger(TimezoneCacheServiceImpl.class);
    private static final TypeReference<List<TimezoneResumeTO>> LIST_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final TimezoneRepository timezoneRepository;
    private final TimezoneMapper timezoneMapper;
    private final ObjectMapper objectMapper;

    public TimezoneCacheServiceImpl(
            StringRedisTemplate redisTemplate,
            TimezoneRepository timezoneRepository,
            TimezoneMapper timezoneMapper,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.timezoneRepository = timezoneRepository;
        this.timezoneMapper = timezoneMapper;
        this.objectMapper = objectMapper;
    }

    /** {@inheritDoc} */
    @Override
    public List<TimezoneResumeTO> getAllTimezones() {
        try {
            return readThroughCache();
        } catch (DataAccessException e) {
            log.warn("timezone.cache.unavailable falling back to direct DB read: {}", e.getMessage());
            return loadFromDb();
        }
    }

    private List<TimezoneResumeTO> readThroughCache() {
        String cached = redisTemplate.opsForValue().get(DirectoryAppConstants.TIMEZONE_CACHE_KEY);
        if (cached != null) {
            log.debug("timezone.cache.hit key={}", DirectoryAppConstants.TIMEZONE_CACHE_KEY);
            return deserialize(cached);
        }
        log.debug("timezone.cache.miss key={}", DirectoryAppConstants.TIMEZONE_CACHE_KEY);

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                DirectoryAppConstants.TIMEZONE_CACHE_LOCK_KEY, "1",
                DirectoryAppConstants.TIMEZONE_CACHE_LOCK_TTL_MS, TimeUnit.MILLISECONDS);

        if (Boolean.TRUE.equals(acquired)) {
            try {
                List<TimezoneResumeTO> list = loadFromDb();
                writeToCache(list);
                return list;
            } finally {
                redisTemplate.delete(DirectoryAppConstants.TIMEZONE_CACHE_LOCK_KEY);
            }
        }

        log.warn("timezone.cache.lock-miss falling back to direct DB read");
        return loadFromDb();
    }

    /** {@inheritDoc} */
    @Override
    public void refreshTimezones() {
        List<TimezoneResumeTO> list = loadFromDb();
        writeToCache(list);
        log.info("timezone.cache.refreshed key={}", DirectoryAppConstants.TIMEZONE_CACHE_KEY);
    }

    /** {@inheritDoc} */
    @Override
    public void invalidateTimezones() {
        redisTemplate.delete(DirectoryAppConstants.TIMEZONE_CACHE_KEY);
        log.info("timezone.cache.invalidated key={}", DirectoryAppConstants.TIMEZONE_CACHE_KEY);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<TimezoneResumeTO> loadFromDb() {
        return timezoneRepository.findAll().stream()
                .map(timezoneMapper::toTimezoneResume)
                .toList();
    }

    private void writeToCache(List<TimezoneResumeTO> list) {
        try {
            String json = objectMapper.writeValueAsString(list);
            redisTemplate.opsForValue().set(
                    DirectoryAppConstants.TIMEZONE_CACHE_KEY, json,
                    DirectoryAppConstants.TIMEZONE_CACHE_DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
            log.info("timezone.cache.populated key={} size={}", DirectoryAppConstants.TIMEZONE_CACHE_KEY, list.size());
        } catch (Exception e) {
            log.error("timezone.cache.write-error", e);
        }
    }

    private List<TimezoneResumeTO> deserialize(String json) {
        try {
            return objectMapper.readValue(json, LIST_TYPE);
        } catch (Exception e) {
            log.error("timezone.cache.deserialize-error", e);
            return Collections.emptyList();
        }
    }
}

