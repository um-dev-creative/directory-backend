package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.GeocodeRequest;
import com.umdc.directory.client.geocoding.GeocodingClient;
import com.umdc.directory.client.geocoding.GeocodingException;
import com.umdc.directory.client.geocoding.GeocodingProperties;
import com.umdc.directory.client.geocoding.to.GeocodingCandidate;
import com.umdc.directory.constant.DirectoryAppConstants;
import com.umdc.directory.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeocodeServiceImplTest {

    @Mock
    private GeocodingClient client;
    private GeocodingProperties properties;
    private GeocodeServiceImpl service;
    private final String token = TestTokens.forUser(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        properties = new GeocodingProperties();
        properties.setEnabled(true);
        service = new GeocodeServiceImpl(client, properties);
    }

    private static GeocodeRequest req(String address) {
        return new GeocodeRequest(address, null, null);
    }

    private String reason(org.springframework.http.ResponseEntity<?> r) {
        return r.getHeaders().getFirst(DirectoryAppConstants.MESSAGE_ERROR_HEADER);
    }

    @Test
    void returnsCandidates() {
        when(client.search("1 Main St, Toronto", "ca", 5))
                .thenReturn(List.of(new GeocodingCandidate("1 Main St", 43.6, -79.3, 0.9)));
        var r = service.geocode(new GeocodeRequest(" 1 Main St ", " Toronto ", null), token);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(1, r.getBody().candidates().size());
        assertEquals(43.6, r.getBody().candidates().getFirst().latitude());
    }

    @Test
    void emptyResultIsOkWithNoCandidates() {
        when(client.search(anyString(), anyString(), anyInt())).thenReturn(List.of());
        var r = service.geocode(req("nowhere"), token);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertTrue(r.getBody().candidates().isEmpty());
    }

    @Test
    void capsAtFiveAndDropsOutOfRange() {
        var many = IntStream.range(0, 8).mapToObj(i -> new GeocodingCandidate("c" + i, 45, -75, null)).toList();
        var withBad = new java.util.ArrayList<>(List.of(new GeocodingCandidate("bad", 95, 0, null),
                new GeocodingCandidate("bad2", 0, 181, null), new GeocodingCandidate("nan", Double.NaN, 0, null)));
        withBad.addAll(many);
        when(client.search(anyString(), anyString(), anyInt())).thenReturn(withBad);
        var r = service.geocode(req("x"), token);
        assertEquals(5, r.getBody().candidates().size());
        assertEquals("c0", r.getBody().candidates().getFirst().label());
    }

    @Test
    void countryIsNormalised() {
        when(client.search(anyString(), eq("us"), anyInt())).thenReturn(List.of());
        assertEquals(HttpStatus.OK, service.geocode(new GeocodeRequest("x", null, " US "), token).getStatusCode());
    }

    @Test
    void invalidCountryIs400() {
        var r = service.geocode(new GeocodeRequest("x", null, "canada"), token);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertEquals(GeocodeServiceImpl.COUNTRY_INVALID, reason(r));
    }

    @Test
    void disabledIs503WithReason() {
        properties.setEnabled(false);
        var r = service.geocode(req("x"), token);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, r.getStatusCode());
        assertEquals(GeocodeServiceImpl.DISABLED, reason(r));
        verifyNoInteractions(client);
    }

    @Test
    void noTokenOrBadTokenIs401() {
        assertEquals(HttpStatus.UNAUTHORIZED, service.geocode(req("x"), null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, service.geocode(req("x"), "garbage").getStatusCode());
        verifyNoInteractions(client);
    }

    @Test
    void invalidInputIs400() {
        assertEquals(HttpStatus.BAD_REQUEST, service.geocode(null, token).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.geocode(req(null), token).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.geocode(req("   "), token).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.geocode(req("a".repeat(201)), token).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.geocode(new GeocodeRequest("x", "c".repeat(201), null), token).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, service.geocode(new GeocodeRequest("x", null, "c".repeat(201)), token).getStatusCode());
        assertEquals(GeocodeServiceImpl.FIELD_TOO_LONG, reason(service.geocode(req("a".repeat(201)), token)));
        verifyNoInteractions(client);
    }

    @Test
    void maxLengthIsAccepted() {
        when(client.search(anyString(), anyString(), anyInt())).thenReturn(List.of());
        assertEquals(HttpStatus.OK, service.geocode(req("a".repeat(200)), token).getStatusCode());
    }

    @Test
    void timeoutIs504() {
        when(client.search(anyString(), anyString(), anyInt())).thenThrow(new GeocodingException(GeocodingException.Kind.TIMEOUT));
        var r = service.geocode(req("x"), token);
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, r.getStatusCode());
        assertEquals(GeocodeServiceImpl.PROVIDER_TIMEOUT, reason(r));
    }

    @Test
    void providerErrorIs502() {
        when(client.search(anyString(), anyString(), anyInt())).thenThrow(new GeocodingException(GeocodingException.Kind.UNAVAILABLE));
        var r = service.geocode(req("x"), token);
        assertEquals(HttpStatus.BAD_GATEWAY, r.getStatusCode());
        assertEquals(GeocodeServiceImpl.PROVIDER_FAILED, reason(r));
    }

    @Test
    void limitFollowsProperty() {
        properties.setMaxCandidates(3);
        when(client.search(anyString(), anyString(), anyInt())).thenReturn(List.of());
        service.geocode(req("x"), token);
        verify(client).search("x", "ca", 3);
        properties.setMaxCandidates(99);
        assertEquals(5, properties.effectiveLimit());
        properties.setMaxCandidates(0);
        assertEquals(1, properties.effectiveLimit());
    }
}
