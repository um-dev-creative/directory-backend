package com.umdc.directory.api.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umdc.directory.api.v1.to.TimezoneResumeTO;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.jpa.entity.TimezoneEntity;
import com.umdc.directory.jpa.repository.TimezoneRepository;
import com.umdc.directory.mapper.TimezoneMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TimezoneCacheServiceImplTest {

    StringRedisTemplate redisTemplate;
    ValueOperations<String, String> valueOps;
    TimezoneRepository repository;
    TimezoneMapper mapper;
    ObjectMapper objectMapper;
    TimezoneCacheServiceImpl service;

    @BeforeEach
    void setup() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        repository = mock(TimezoneRepository.class);
        mapper = Mappers.getMapper(TimezoneMapper.class);
        objectMapper = new ObjectMapper();

        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        service = new TimezoneCacheServiceImpl(redisTemplate, repository, mapper, objectMapper);
    }

    private static TimezoneEntity sampleEntity() {
        TimezoneEntity e = new TimezoneEntity();
        e.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        e.setName("UTC");
        e.setAbbreviation("UTC");
        e.setUtcOffset(Duration.ZERO);
        e.setCreatedAt(LocalDateTime.of(2024, 1, 1, 0, 0));
        e.setLastUpdated(LocalDateTime.of(2024, 1, 1, 0, 0));
        return e;
    }

    // ── getAllTimezones ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllTimezones: Redis down falls back to the database instead of failing")
    void getAllTimezones_redisUnavailable_fallsBackToDb() {
        when(valueOps.get(DirectoryAppConstants.TIMEZONE_CACHE_KEY))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("down"));
        when(repository.findAll()).thenReturn(List.of(sampleEntity()));

        List<TimezoneResumeTO> result = service.getAllTimezones();

        assertEquals(1, result.size());
        assertEquals("UTC", result.get(0).name());
        verify(repository).findAll();
    }

    @Test
    @DisplayName("getAllTimezones: cache hit returns cached list and does not call repository")
    void getAllTimezones_cacheHit_returnsListWithoutDbCall() throws Exception {
        List<TimezoneResumeTO> expected = List.of(
                new TimezoneResumeTO(UUID.fromString("00000000-0000-0000-0000-000000000001"), "UTC", "UTC"));
        String json = objectMapper.writeValueAsString(expected);

        when(valueOps.get(DirectoryAppConstants.TIMEZONE_CACHE_KEY)).thenReturn(json);

        List<TimezoneResumeTO> result = service.getAllTimezones();

        assertEquals(1, result.size());
        assertEquals("UTC", result.get(0).name());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("getAllTimezones: cache miss with lock acquired loads from DB and writes to Redis")
    void getAllTimezones_cacheMiss_lockAcquired_populatesCache() {
        when(valueOps.get(DirectoryAppConstants.TIMEZONE_CACHE_KEY)).thenReturn(null);
        when(valueOps.setIfAbsent(
                eq(DirectoryAppConstants.TIMEZONE_CACHE_LOCK_KEY), eq("1"),
                eq(DirectoryAppConstants.TIMEZONE_CACHE_LOCK_TTL_MS), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(Boolean.TRUE);
        when(repository.findAll()).thenReturn(List.of(sampleEntity()));

        List<TimezoneResumeTO> result = service.getAllTimezones();

        assertEquals(1, result.size());
        verify(repository, times(1)).findAll();
        verify(valueOps, times(1)).set(
                eq(DirectoryAppConstants.TIMEZONE_CACHE_KEY), anyString(),
                eq(DirectoryAppConstants.TIMEZONE_CACHE_DEFAULT_TTL_SECONDS), eq(TimeUnit.SECONDS));
        verify(redisTemplate, times(1)).delete(DirectoryAppConstants.TIMEZONE_CACHE_LOCK_KEY);
    }

    @Test
    @DisplayName("getAllTimezones: cache miss with lock not acquired falls back to direct DB read")
    void getAllTimezones_cacheMiss_lockNotAcquired_fallsBackToDb() {
        when(valueOps.get(DirectoryAppConstants.TIMEZONE_CACHE_KEY)).thenReturn(null);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(Boolean.FALSE);
        when(repository.findAll()).thenReturn(List.of(sampleEntity()));

        List<TimezoneResumeTO> result = service.getAllTimezones();

        assertEquals(1, result.size());
        verify(repository, times(1)).findAll();
        verify(valueOps, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("getAllTimezones: deserialize error on bad JSON returns empty list")
    void getAllTimezones_badJson_returnsEmptyList() {
        when(valueOps.get(DirectoryAppConstants.TIMEZONE_CACHE_KEY)).thenReturn("NOT_JSON{{{");

        List<TimezoneResumeTO> result = service.getAllTimezones();

        assertTrue(result.isEmpty());
        verifyNoInteractions(repository);
    }

    // ── refreshTimezones ─────────────────────────────────────────────────────

    @Test
    @DisplayName("refreshTimezones: loads from DB and writes to Redis")
    void refreshTimezones_writesToCache() {
        when(repository.findAll()).thenReturn(List.of(sampleEntity()));

        service.refreshTimezones();

        verify(repository, times(1)).findAll();
        verify(valueOps, times(1)).set(
                eq(DirectoryAppConstants.TIMEZONE_CACHE_KEY), anyString(),
                eq(DirectoryAppConstants.TIMEZONE_CACHE_DEFAULT_TTL_SECONDS), eq(TimeUnit.SECONDS));
    }

    // ── invalidateTimezones ───────────────────────────────────────────────────

    @Test
    @DisplayName("invalidateTimezones: deletes the Redis cache key")
    void invalidateTimezones_deletesKey() {
        service.invalidateTimezones();

        verify(redisTemplate, times(1)).delete(DirectoryAppConstants.TIMEZONE_CACHE_KEY);
    }

    @Test
    @DisplayName("invalidateTimezones: subsequent getAllTimezones triggers DB reload")
    void invalidateTimezones_thenGetAll_reloadsFromDb() {
        // After invalidation the cache is empty → next getAllTimezones loads from DB
        service.invalidateTimezones();

        when(valueOps.get(DirectoryAppConstants.TIMEZONE_CACHE_KEY)).thenReturn(null);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(Boolean.TRUE);
        when(repository.findAll()).thenReturn(List.of(sampleEntity()));

        List<TimezoneResumeTO> result = service.getAllTimezones();

        assertEquals(1, result.size());
        verify(repository, times(1)).findAll();
    }
}

