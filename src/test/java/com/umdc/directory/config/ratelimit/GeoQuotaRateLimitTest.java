package com.umdc.directory.config.ratelimit;

import com.umdc.directory.api.v1.controller.BusinessGeoController;
import com.umdc.directory.api.v1.controller.GeocodeController;
import com.umdc.directory.api.v1.service.BusinessGeoService;
import com.umdc.directory.api.v1.service.GeocodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GeoQuotaRateLimitTest {

    private final AtomicLong clock = new AtomicLong();

    private static String token(String uid) {
        String payload = "{\"uid\":\"" + uid + "\"}";
        return "h." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".s";
    }

    private RateLimitFilter filter(int ip, int user, int geocode, int map, int nearby) {
        return new RateLimitFilter(new InMemoryRateLimitStore(clock::get), "/api/v1", ip, user, false, 0, geocode, map, nearby);
    }

    private static MockHttpServletRequest req(String method, String uri, String ip, String uid) {
        MockHttpServletRequest r = new MockHttpServletRequest(method, uri);
        r.setRequestURI(uri);
        r.setRemoteAddr(ip);
        if (uid != null) {
            r.addHeader("session-token", token(uid));
        }
        return r;
    }

    private static MockHttpServletResponse call(RateLimitFilter f, MockHttpServletRequest req) throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req, res, new MockFilterChain());
        return res;
    }

    @Test
    void geocodeRejectsWith429RetryAfterAndMessageHeader() throws Exception {
        RateLimitFilter f = filter(1000, 1000, 2, 60, 60);
        assertThat(call(f, req("POST", "/api/v1/geocode", "1.1.1.1", null)).getStatus()).isEqualTo(200);
        assertThat(call(f, req("POST", "/api/v1/geocode", "1.1.1.1", null)).getStatus()).isEqualTo(200);
        MockHttpServletResponse res = call(f, req("POST", "/api/v1/geocode", "1.1.1.1", null));
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isEqualTo("30");
        assertThat(res.getHeader("X-RateLimit-Limit")).isEqualTo("2");
        assertThat(res.getHeader("message")).contains("Too many requests").contains("30");
        // another IP has its own bucket
        assertThat(call(f, req("POST", "/api/v1/geocode", "2.2.2.2", null)).getStatus()).isEqualTo(200);
    }

    @Test
    void bucketsAreKeyedByUserWhenATokenIsPresentAcrossIps() throws Exception {
        RateLimitFilter f = filter(1000, 1000, 1, 60, 60);
        assertThat(call(f, req("POST", "/api/v1/geocode", "1.1.1.1", "u1")).getStatus()).isEqualTo(200);
        assertThat(call(f, req("POST", "/api/v1/geocode", "9.9.9.9", "u1")).getStatus()).isEqualTo(429);
        assertThat(call(f, req("POST", "/api/v1/geocode", "1.1.1.1", "u2")).getStatus()).isEqualTo(200);
        // anonymous traffic from the same IP has a separate (IP) bucket
        assertThat(call(f, req("POST", "/api/v1/geocode", "1.1.1.1", null)).getStatus()).isEqualTo(200);
    }

    @Test
    void mapAndNearbyHaveIndependentBuckets() throws Exception {
        RateLimitFilter f = filter(1000, 1000, 10, 1, 2);
        assertThat(call(f, req("GET", "/api/v1/businesses/map", "1.1.1.1", null)).getStatus()).isEqualTo(200);
        assertThat(call(f, req("GET", "/api/v1/businesses/map", "1.1.1.1", null)).getStatus()).isEqualTo(429);
        assertThat(call(f, req("GET", "/api/v1/businesses/nearby", "1.1.1.1", null)).getStatus()).isEqualTo(200);
        assertThat(call(f, req("GET", "/api/v1/businesses/nearby", "1.1.1.1", null)).getStatus()).isEqualTo(200);
        MockHttpServletResponse res = call(f, req("GET", "/api/v1/businesses/nearby", "1.1.1.1", null));
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("X-RateLimit-Limit")).isEqualTo("2");
        assertThat(call(f, req("POST", "/api/v1/geocode", "1.1.1.1", null)).getStatus()).isEqualTo(200);
    }

    @Test
    void otherRoutesAndMethodsAreNotAffected() throws Exception {
        RateLimitFilter f = filter(1000, 1000, 1, 1, 1);
        for (int i = 0; i < 4; i++) {
            assertThat(call(f, req("GET", "/api/v1/geocode", "1.1.1.1", null)).getStatus()).isEqualTo(200);
            assertThat(call(f, req("POST", "/api/v1/businesses/map", "1.1.1.1", null)).getStatus()).isEqualTo(200);
            assertThat(call(f, req("POST", "/api/v1/businesses/nearby", "1.1.1.1", null)).getStatus()).isEqualTo(200);
            assertThat(call(f, req("GET", "/api/v1/businesses/map/extra", "1.1.1.1", null)).getStatus()).isEqualTo(200);
            assertThat(call(f, req("GET", "/api/v1/businesses", "1.1.1.1", null)).getStatus()).isEqualTo(200);
        }
    }

    @Test
    void zeroQuotaDisablesTheBucketAndLegacyConstructorsKeepItOff() throws Exception {
        RateLimitFilter zero = filter(1000, 1000, 0, -1, 0);
        RateLimitFilter legacy = new RateLimitFilter(new InMemoryRateLimitStore(clock::get), "/api/v1", 1000, 1000, false, 5);
        for (int i = 0; i < 5; i++) {
            assertThat(call(zero, req("POST", "/api/v1/geocode", "1.1.1.1", null)).getStatus()).isEqualTo(200);
            assertThat(call(zero, req("GET", "/api/v1/businesses/map", "1.1.1.1", null)).getStatus()).isEqualTo(200);
            assertThat(call(zero, req("GET", "/api/v1/businesses/nearby", "1.1.1.1", null)).getStatus()).isEqualTo(200);
            assertThat(call(legacy, req("POST", "/api/v1/geocode", "1.1.1.1", null)).getStatus()).isEqualTo(200);
        }
    }

    @Test
    void globalIpAndUserBucketsStillApplyFirst() throws Exception {
        RateLimitFilter ipFirst = filter(1, 1000, 50, 50, 50);
        assertThat(call(ipFirst, req("POST", "/api/v1/geocode", "1.1.1.1", null)).getStatus()).isEqualTo(200);
        assertThat(call(ipFirst, req("POST", "/api/v1/geocode", "1.1.1.1", null)).getStatus()).isEqualTo(429);

        RateLimitFilter userFirst = filter(1000, 1, 50, 50, 50);
        assertThat(call(userFirst, req("GET", "/api/v1/businesses/map", "1.1.1.1", "u1")).getStatus()).isEqualTo(200);
        MockHttpServletResponse res = call(userFirst, req("GET", "/api/v1/businesses/map", "1.1.1.1", "u1"));
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("X-RateLimit-Limit")).isEqualTo("1");
    }

    @Test
    void quotaRefillsOverTime() throws Exception {
        RateLimitFilter f = filter(1000, 1000, 1, 60, 60);
        assertThat(call(f, req("POST", "/api/v1/geocode", "1.1.1.1", null)).getStatus()).isEqualTo(200);
        assertThat(call(f, req("POST", "/api/v1/geocode", "1.1.1.1", null)).getStatus()).isEqualTo(429);
        clock.addAndGet(61_000_000_000L);
        assertThat(call(f, req("POST", "/api/v1/geocode", "1.1.1.1", null)).getStatus()).isEqualTo(200);
    }

    @Test
    void rejectedGeocodeAndGeoRequestsNeverReachTheServices() throws Exception {
        GeocodeService geocode = Mockito.mock(GeocodeService.class);
        BusinessGeoService geo = Mockito.mock(BusinessGeoService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new GeocodeController(geocode), new BusinessGeoController(geo))
                .addFilters(filter(1000, 1000, 1, 1, 1)).build();

        mvc.perform(post("/api/v1/geocode").contentType(MediaType.APPLICATION_JSON).content("{\"address\":\"x\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/geocode").contentType(MediaType.APPLICATION_JSON).content("{\"address\":\"x\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().exists("message"));
        assertThat(Mockito.mockingDetails(geocode).getInvocations()).hasSize(1);

        mvc.perform(get("/api/v1/businesses/map")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/businesses/map")).andExpect(status().isTooManyRequests());
        assertThat(Mockito.mockingDetails(geo).getInvocations()).hasSize(1);

        mvc.perform(get("/api/v1/businesses/nearby")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/businesses/nearby")).andExpect(status().isTooManyRequests());
        assertThat(Mockito.mockingDetails(geo).getInvocations()).hasSize(2);
    }

    private InMemoryRateLimitStore store;

    @BeforeEach
    void freshStore() {
        store = new InMemoryRateLimitStore(clock::get, 3);
    }

    @Test
    void storeStaysBoundedUnderAFloodOfDistinctKeys() {
        for (int i = 0; i < 50; i++) {
            store.tryConsume("k" + i, 1);
            assertThat(store.size()).isLessThanOrEqualTo(3);
        }
    }

    @Test
    void storeEvictsIdleBucketsBeforeTrimmingActiveOnes() {
        store.tryConsume("a", 60);
        store.tryConsume("b", 60);
        store.tryConsume("c", 60);
        clock.addAndGet(120_000_000_000L); // all refilled: idle
        store.tryConsume("d", 60);
        assertThat(store.size()).isEqualTo(1);
        // an existing key never triggers trimming
        store.tryConsume("d", 60);
        assertThat(store.size()).isEqualTo(1);
    }

    @Test
    void storeMaxBucketsHasAFloorOfOne() {
        InMemoryRateLimitStore tiny = new InMemoryRateLimitStore(clock::get, 0);
        tiny.tryConsume("a", 1);
        tiny.tryConsume("b", 1);
        assertThat(tiny.size()).isEqualTo(1);
    }
}
