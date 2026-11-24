package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.service.GeoSearchValidator.MapQuery;
import com.umdc.directory.api.v1.service.GeoSearchValidator.NearbyQuery;
import com.umdc.directory.api.v1.to.BusinessMapResponse;
import com.umdc.directory.api.v1.to.BusinessNearbyResponse;
import com.umdc.directory.jpa.repository.LocationGeoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BusinessGeoServiceCacheTest {

    private LocationGeoRepository repository;
    private GeoResponseCache cache;
    private BusinessGeoServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(LocationGeoRepository.class);
        cache = mock(GeoResponseCache.class);
        when(cache.enabled()).thenReturn(true);
        service = new BusinessGeoServiceImpl(repository, cache);
    }

    @Test
    void nearbyHitDoesNotTouchTheDatabase() {
        var cached = new BusinessNearbyResponse(1, 1, 10, 1, List.of());
        when(cache.get(startsWith("umdc:directory:geo:nearby:v1:"), eq(BusinessNearbyResponse.class))).thenReturn(Optional.of(cached));
        assertEquals(cached, service.nearby("43.6512", "-79.3801", null, null, null, null, null, null, null).getBody());
        verifyNoInteractions(repository);
        verify(cache, never()).put(any(), any());
    }

    @Test
    void nearbyMissQueriesTheSnappedCentreAndStoresTheResult() {
        when(cache.get(any(), eq(BusinessNearbyResponse.class))).thenReturn(Optional.empty());
        when(repository.countNearby(anyDouble(), anyDouble(), anyInt(), any(), any(), any())).thenReturn(0L);
        var body = service.nearby("43.6512", "-79.3801", null, null, null, null, null, null, null).getBody();
        verify(repository).countNearby(eq(43.651), eq(-79.38), eq(5000), any(), any(), any());
        verify(cache).put(startsWith("umdc:directory:geo:nearby:v1:"), eq(body));
    }

    @Test
    void mapHitDoesNotTouchTheDatabase() {
        var cached = new BusinessMapResponse("FeatureCollection", List.of(), false, false, 14);
        when(cache.get(startsWith("umdc:directory:geo:map:v1:"), eq(BusinessMapResponse.class))).thenReturn(Optional.of(cached));
        assertEquals(cached, service.map("-80,43,-79,44", "14", null, null, null, null).getBody());
        verifyNoInteractions(repository);
    }

    @Test
    void mapMissQueriesTheGridExpandedBboxAndStoresTheResult() {
        when(cache.get(any(), eq(BusinessMapResponse.class))).thenReturn(Optional.empty());
        when(repository.findInBbox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(), any(), any(), anyInt())).thenReturn(List.of());
        var body = service.map("-79.99991,43.00001,-79.00001,43.99999", "14", null, null, null, null).getBody();
        // zoom 14: step max(0.0005, 90/16384/4) = 0.0005 -> bbox widened outwards to the grid
        var min = ArgumentCaptor.forClass(Double.class);
        verify(repository).findInBbox(min.capture(), anyDouble(), anyDouble(), anyDouble(), any(), any(), any(), eq(201));
        assertTrue(min.getValue() <= -79.99991);
        verify(cache).put(startsWith("umdc:directory:geo:map:v1:"), eq(body));
    }

    @Test
    void invalidInputIsStillA400WithTheCacheOn() {
        assertEquals(400, service.map("bad", null, null, null, null, null).getStatusCode().value());
        assertEquals(400, service.nearby("x", "0", null, null, null, null, null, null, null).getStatusCode().value());
        verifyNoInteractions(repository);
        verify(cache, never()).get(any(), any());
    }

    @Test
    void nearbyKeysDependOnTheQueryOnlyAndAreBounded() {
        NearbyQuery a = BusinessGeoServiceImpl.snap(GeoSearchValidator.nearby("43.6512", "-79.3801", null, null, null, "bakery", null, null, null));
        NearbyQuery b = BusinessGeoServiceImpl.snap(GeoSearchValidator.nearby("43.6514", "-79.3799", null, null, null, "bakery", null, null, null));
        NearbyQuery c = BusinessGeoServiceImpl.snap(GeoSearchValidator.nearby("43.6512", "-79.3801", null, null, null, "bread", "name", "2", "5"));
        assertEquals(BusinessGeoServiceImpl.nearbyKey(a), BusinessGeoServiceImpl.nearbyKey(b)); // same grid cell
        assertNotEquals(BusinessGeoServiceImpl.nearbyKey(a), BusinessGeoServiceImpl.nearbyKey(c));
        String longText = "x".repeat(100);
        NearbyQuery d = BusinessGeoServiceImpl.snap(GeoSearchValidator.nearby("1", "2", null, UUID.randomUUID().toString(), UUID.randomUUID().toString(), longText, null, null, null));
        assertTrue(BusinessGeoServiceImpl.nearbyKey(d).length() < 120);
    }

    @Test
    void mapKeysShareTheGridCellAndDifferByFilters() {
        MapQuery a = BusinessGeoServiceImpl.snap(GeoSearchValidator.map("-79.99991,43.00001,-79.00001,43.99999", "14", null, null, null, null));
        MapQuery b = BusinessGeoServiceImpl.snap(GeoSearchValidator.map("-79.99990,43.00002,-79.00002,43.99998", "14", null, null, null, null));
        MapQuery c = BusinessGeoServiceImpl.snap(GeoSearchValidator.map("-79.99991,43.00001,-79.00001,43.99999", "14", null, null, "cafe", "50"));
        assertEquals(BusinessGeoServiceImpl.mapKey(a), BusinessGeoServiceImpl.mapKey(b));
        assertNotEquals(BusinessGeoServiceImpl.mapKey(a), BusinessGeoServiceImpl.mapKey(c));
        MapQuery world = BusinessGeoServiceImpl.snap(GeoSearchValidator.map("-180,0,-100,90", "1", null, null, null, null));
        assertEquals(-180d, world.minLng());
        assertEquals(90d, world.maxLat());
        assertEquals(0d, world.minLat());
    }

    @Test
    void aNullCacheMeansNoCaching() {
        var plain = new BusinessGeoServiceImpl(repository, null);
        when(repository.countNearby(anyDouble(), anyDouble(), anyInt(), any(), any(), any())).thenReturn(0L);
        plain.nearby("43.6512", "-79.3801", null, null, null, null, null, null, null);
        verify(repository).countNearby(eq(43.6512), eq(-79.3801), eq(5000), any(), any(), any());
    }
}
