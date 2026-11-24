package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.BusinessMapResponse;
import com.umdc.directory.api.v1.to.GeoPointTO;
import com.umdc.directory.api.v1.to.MapFeatureTO;
import com.umdc.directory.config.ratelimit.RedisFailOpenGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class GeoResponseCacheTest {

    private final AtomicLong millis = new AtomicLong(1_000);
    private StringRedisTemplate template;
    private ValueOperations<String, String> ops;
    private GeoResponseCache cache;

    private final BusinessMapResponse value = new BusinessMapResponse("FeatureCollection",
            List.of(new MapFeatureTO("Feature", GeoPointTO.of(-79.38, 43.65), Map.of("name", "Biz"))), false, false, 15);

    @BeforeEach
    void setUp() {
        template = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        cache = new GeoResponseCache(template, true, 60, new RedisFailOpenGuard("t", millis::get, 5_000));
    }

    @Test
    void putStoresJsonWithTheTtlAndGetReadsItBack() {
        cache.put("k", value);
        verify(ops).set(eq("k"), anyString(), eq(Duration.ofSeconds(60)));
        when(ops.get("k")).thenReturn("{\"type\":\"FeatureCollection\",\"features\":[],\"truncated\":true,\"clustered\":false,\"zoom\":3}");
        var hit = cache.get("k", BusinessMapResponse.class);
        assertThat(hit).isPresent();
        assertThat(hit.get().truncated()).isTrue();
        assertThat(hit.get().zoom()).isEqualTo(3);
    }

    @Test
    void aMissIsEmpty() {
        assertThat(cache.get("k", BusinessMapResponse.class)).isEmpty();
    }

    @Test
    void aCorruptEntryIsAMiss() {
        when(ops.get("k")).thenReturn("not json");
        assertThat(cache.get("k", BusinessMapResponse.class)).isEmpty();
    }

    @Test
    void redisFailuresFailOpenAndOpenTheBreaker() {
        when(ops.get(anyString())).thenThrow(new QueryTimeoutException("down"));
        assertThat(cache.get("k", BusinessMapResponse.class)).isEmpty();
        assertThat(cache.get("k", BusinessMapResponse.class)).isEmpty();
        cache.put("k", value);
        verify(ops, times(1)).get("k"); // breaker open: no second call, no timeout wait
        verify(ops, never()).set(anyString(), anyString(), any(Duration.class));
        millis.addAndGet(5_000);
        org.mockito.Mockito.doReturn(null).when(ops).get(anyString());
        assertThat(cache.get("k", BusinessMapResponse.class)).isEmpty();
        verify(ops, times(2)).get("k");
    }

    @Test
    void aFailingWriteIsSwallowed() {
        org.mockito.Mockito.doThrow(new QueryTimeoutException("down")).when(ops).set(anyString(), anyString(), any(Duration.class));
        cache.put("k", value);
        cache.put("k2", value);
        verify(ops, times(1)).set(eq("k"), anyString(), any(Duration.class));
    }

    @Test
    void anUnserialisableValueIsSkipped() {
        cache.put("k", new Object());
        verify(ops, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void anOversizedValueIsNotStored() {
        cache.put("k", "x".repeat(GeoResponseCache.MAX_VALUE_BYTES));
        verify(ops, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void disabledOrWithoutRedisItDoesNothing() {
        GeoResponseCache off = new GeoResponseCache(template, false, 60, new RedisFailOpenGuard("t"));
        assertThat(off.enabled()).isFalse();
        off.put("k", value);
        assertThat(off.get("k", BusinessMapResponse.class)).isEmpty();
        assertThat(GeoResponseCache.disabled().enabled()).isFalse();
        ObjectProvider<StringRedisTemplate> none = mock(ObjectProvider.class);
        assertThat(new GeoResponseCache(none, true, 60).enabled()).isFalse();
        ObjectProvider<StringRedisTemplate> some = mock(ObjectProvider.class);
        when(some.getIfAvailable()).thenReturn(template);
        assertThat(new GeoResponseCache(some, true, 0).enabled()).isTrue();
        verify(ops, never()).set(anyString(), anyString(), any(Duration.class));
    }
}
