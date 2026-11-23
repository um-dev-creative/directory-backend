package com.umdc.directory.client.geocoding;

import com.umdc.directory.client.geocoding.to.MaptilerResponse;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignGeocodingClientTest {

    @Mock
    private MaptilerGeocodingFeign feign;
    private FeignGeocodingClient client;

    @BeforeEach
    void setUp() {
        var props = new GeocodingProperties();
        props.setApiKey("SECRET-KEY");
        client = new FeignGeocodingClient(feign, props);
    }

    private static Request request() {
        return Request.create(Request.HttpMethod.GET, "https://x/geocoding/a.json?key=SECRET-KEY", Map.of(),
                new byte[0], StandardCharsets.UTF_8, null);
    }

    @Test
    void mapsFeaturesAndSwapsCenter() {
        var f = new MaptilerResponse.Feature("1 Main St", List.of(-79.3, 43.6), 0.8);
        when(feign.geocode(anyString(), eq("SECRET-KEY"), eq(5), eq("ca"))).thenReturn(new MaptilerResponse(List.of(f)));
        var out = client.search("1 Main", "ca", 5);
        assertEquals(1, out.size());
        assertEquals(43.6, out.getFirst().latitude());
        assertEquals(-79.3, out.getFirst().longitude());
        assertEquals("1 Main St", out.getFirst().label());
    }

    @Test
    void skipsMalformedFeaturesAndHandlesEmpty() {
        var bad = Arrays.asList(
                new MaptilerResponse.Feature("a", null, null),
                new MaptilerResponse.Feature("b", List.of(1.0), null),
                new MaptilerResponse.Feature("c", Arrays.asList(null, 1.0), null),
                null);
        when(feign.geocode(anyString(), anyString(), anyInt(), anyString())).thenReturn(new MaptilerResponse(bad));
        assertTrue(client.search("x", "ca", 5).isEmpty());
        when(feign.geocode(anyString(), anyString(), anyInt(), anyString())).thenReturn(new MaptilerResponse(null));
        assertTrue(client.search("x", "ca", 5).isEmpty());
        when(feign.geocode(anyString(), anyString(), anyInt(), anyString())).thenReturn(null);
        assertTrue(client.search("x", "ca", 5).isEmpty());
        assertEquals(List.of(), Collections.emptyList());
    }

    @Test
    void timeoutBecomesTimeoutKind() {
        var e = new RetryableException(-1, "Read timed out executing GET https://x?key=SECRET-KEY", Request.HttpMethod.GET,
                new SocketTimeoutException("Read timed out"), (Date) null, request());
        when(feign.geocode(anyString(), anyString(), anyInt(), anyString())).thenThrow(e);
        var ex = assertThrows(GeocodingException.class, () -> client.search("x", "ca", 5));
        assertEquals(GeocodingException.Kind.TIMEOUT, ex.getKind());
        assertFalse(ex.getMessage().contains("SECRET"));
    }

    @Test
    void serverErrorBecomesUnavailableWithoutLeakingKey() {
        var e = FeignException.errorStatus("geocode", Response.builder().status(503).reason("x")
                .request(request()).headers(Map.of()).build());
        when(feign.geocode(anyString(), anyString(), anyInt(), anyString())).thenThrow(e);
        var ex = assertThrows(GeocodingException.class, () -> client.search("x", "ca", 5));
        assertEquals(GeocodingException.Kind.UNAVAILABLE, ex.getKind());
        assertFalse(ex.getMessage().contains("SECRET"));
        assertFalse(String.valueOf(ex.getCause()).contains("SECRET"));
    }

    @Test
    void genericRuntimeErrorIsUnavailable() {
        when(feign.geocode(anyString(), anyString(), anyInt(), anyString())).thenThrow(new IllegalStateException("boom"));
        assertEquals(GeocodingException.Kind.UNAVAILABLE,
                assertThrows(GeocodingException.class, () -> client.search("x", "ca", 5)).getKind());
    }

    @Test
    void queryIsSanitisedForThePath() {
        when(feign.geocode(anyString(), anyString(), anyInt(), anyString())).thenReturn(null);
        client.search("a/b?c#d;e\\f", "ca", 5);
        verify(feign).geocode("a b c d e f", "SECRET-KEY", 5, "ca");
    }

    @Test
    void feignConfigurationUsesPropertyTimeouts() {
        var props = new GeocodingProperties();
        props.setConnectTimeoutMs(111);
        props.setReadTimeoutMs(222);
        var options = new GeocodingFeignConfiguration().geocodingRequestOptions(props);
        assertEquals(111, options.connectTimeoutMillis());
        assertEquals(222, options.readTimeoutMillis());
    }

    @Test
    void defaultsAreSafe() {
        var p = new GeocodingProperties();
        assertFalse(p.isEnabled());
        assertEquals("", p.getApiKey());
        assertEquals("https://api.maptiler.com", p.getBaseUrl());
        p.setBaseUrl("https://other");
        assertEquals("https://other", p.getBaseUrl());
        assertEquals(5, p.getMaxCandidates());
    }
}
