package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.BusinessMapResponse;
import com.umdc.directory.api.v1.to.BusinessNearbyResponse;
import com.umdc.directory.jpa.repository.LocationGeoRepository;
import com.umdc.directory.jpa.repository.LocationGeoRepository.ClusterRow;
import com.umdc.directory.jpa.repository.LocationGeoRepository.MapPointRow;
import com.umdc.directory.jpa.repository.LocationGeoRepository.NearbyRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessGeoServiceImplTest {

    @Mock
    private LocationGeoRepository repository;
    @InjectMocks
    private BusinessGeoServiceImpl service;

    private NearbyRow row(double distance) {
        return new NearbyRow(UUID.randomUUID(), UUID.randomUUID(), "Biz", "d", UUID.randomUUID(), "Cat", true, null, UUID.randomUUID(),
                "Toronto", null, "1 Main", 43.65, -79.38, true, distance);
    }

    @Test
    void nearbyReturnsAPageWithRoundedDistanceAndTotals() {
        when(repository.countNearby(eq(43.65), eq(-79.38), eq(5000), isNull(), isNull(), isNull())).thenReturn(25L);
        when(repository.findNearby(eq(43.65), eq(-79.38), eq(5000), isNull(), isNull(), isNull(), eq(false), eq(10), eq(10)))
                .thenReturn(List.of(row(1234.6)));

        var response = service.nearby("43.65", "-79.38", null, null, null, null, null, "2", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BusinessNearbyResponse body = response.getBody();
        assertEquals(25, body.totalCount());
        assertEquals(3, body.totalPages());
        assertEquals(2, body.page());
        assertEquals(10, body.perPage());
        assertEquals(1235L, body.data().iterator().next().distanceM());
    }

    @Test
    void nearbyPassesTheSortAndFilters() {
        var cat = UUID.randomUUID();
        when(repository.countNearby(anyDouble(), anyDouble(), anyInt(), any(), any(), any())).thenReturn(0L);
        when(repository.findNearby(anyDouble(), anyDouble(), anyInt(), any(), any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(List.of());
        var response = service.nearby("1", "2", "100", cat.toString(), null, "x", "name", null, "5");
        assertEquals(0, response.getBody().totalPages());
        verify(repository).findNearby(1d, 2d, 100, cat, null, "x", true, 5, 0);
    }

    @Test
    void nearbyInvalidParametersAreA400WithTheReason() {
        var response = service.nearby("100", "0", null, null, null, null, null, null, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getHeaders().getFirst("message-error").startsWith("lat must be between"));
        verifyNoInteractions(repository);
    }

    @Test
    void mapReturnsPointsAtHighZoom() {
        var point = new MapPointRow(UUID.randomUUID(), UUID.randomUUID(), "Biz", UUID.randomUUID(), true, false, 43.653219, -79.383187);
        when(repository.findInBbox(eq(-80d), eq(43d), eq(-79d), eq(44d), isNull(), isNull(), isNull(), eq(201))).thenReturn(List.of(point));

        BusinessMapResponse body = service.map("-80,43,-79,44", "14", null, null, null, null).getBody();

        assertEquals("FeatureCollection", body.type());
        assertFalse(body.truncated());
        assertFalse(body.clustered());
        var feature = body.features().get(0);
        assertEquals("Feature", feature.type());
        assertEquals("Point", feature.geometry().type());
        assertEquals(List.of(-79.38319, 43.65322), feature.geometry().coordinates());
        assertEquals(point.businessId(), feature.properties().get("business_id"));
        assertEquals(point.locationId(), feature.properties().get("location_id"));
        assertEquals(false, feature.properties().get("main"));
        assertEquals(true, feature.properties().get("verified"));
    }

    @Test
    void mapIsTruncatedWhenTheLimitIsReached() {
        List<MapPointRow> rows = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            rows.add(new MapPointRow(UUID.randomUUID(), UUID.randomUUID(), "B", UUID.randomUUID(), false, true, 43.1, -79.1));
        }
        when(repository.findInBbox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(), any(), any(), eq(3))).thenReturn(rows);
        var body = service.map("-80,43,-79,44", "20", null, null, null, "2").getBody();
        assertTrue(body.truncated());
        assertEquals(2, body.features().size());
    }

    @Test
    void mapReturnsClustersBelowZoom14() {
        when(repository.findClusters(eq(-80d), eq(43d), eq(-79d), eq(44d), isNull(), isNull(), isNull(), eq(90d / 1024d), eq(3)))
                .thenReturn(List.of(new ClusterRow(7, 43.5, -79.5), new ClusterRow(2, 43.6, -79.6)));
        var body = service.map("-80,43,-79,44", "10", null, null, null, "2").getBody();
        assertTrue(body.clustered());
        assertFalse(body.truncated());
        assertEquals(10, body.zoom());
        assertEquals(7L, body.features().get(0).properties().get("count"));
        assertEquals(true, body.features().get(0).properties().get("cluster"));
        assertEquals(List.of(-79.5, 43.5), body.features().get(0).geometry().coordinates());
    }

    @Test
    void clustersAreTruncatedAtTheLimit() {
        when(repository.findClusters(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(), any(), any(), anyDouble(), eq(2)))
                .thenReturn(List.of(new ClusterRow(7, 1, 1), new ClusterRow(2, 2, 2)));
        var body = service.map("-80,43,-79,44", "3", null, null, null, "1").getBody();
        assertTrue(body.truncated());
        assertEquals(1, body.features().size());
    }

    @Test
    void mapInvalidBboxIsA400() {
        var response = service.map("-79,43,-80,44", null, null, null, null, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getHeaders().getFirst("message-error").startsWith("bbox is inverted"));
        verifyNoInteractions(repository);
    }

    @Test
    void theDefaultServiceIsNotImplemented() {
        var empty = new BusinessGeoService() {};
        assertEquals(HttpStatus.NOT_IMPLEMENTED, empty.nearby(null, null, null, null, null, null, null, null, null).getStatusCode());
        assertEquals(HttpStatus.NOT_IMPLEMENTED, empty.map(null, null, null, null, null, null).getStatusCode());
    }
}
