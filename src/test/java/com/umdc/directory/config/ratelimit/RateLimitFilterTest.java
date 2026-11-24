package com.umdc.directory.config.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private static String token(String payload) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        return "h." + enc.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".s";
    }

    private static RateLimitFilter filter(int ip, int user, boolean forwarded) {
        return new RateLimitFilter(new InMemoryRateLimitStore(new AtomicLong()::get), "/api/v1", ip, user, forwarded);
    }

    private static MockHttpServletResponse call(RateLimitFilter f, MockHttpServletRequest req) throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req, res, new MockFilterChain());
        return res;
    }

    private static MockHttpServletRequest req(String uri, String ip) {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", uri);
        r.setRequestURI(uri);
        r.setRemoteAddr(ip);
        return r;
    }

    @Test
    void rejectsWith429AndRetryAfterOnceTheIpQuotaIsSpent() throws Exception {
        RateLimitFilter f = filter(2, 100, false);
        assertThat(call(f, req("/api/v1/businesses", "1.1.1.1")).getStatus()).isEqualTo(200);
        assertThat(call(f, req("/api/v1/businesses", "1.1.1.1")).getStatus()).isEqualTo(200);
        MockHttpServletResponse res = call(f, req("/api/v1/businesses", "1.1.1.1"));
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isEqualTo("30");
        assertThat(res.getHeader("X-RateLimit-Limit")).isEqualTo("2");
        assertThat(res.getContentAsString()).contains("too_many_requests");
        assertThat(call(f, req("/api/v1/businesses", "2.2.2.2")).getStatus()).isEqualTo(200);
    }

    @Test
    void neverLimitsHealthOrOtherPaths() throws Exception {
        RateLimitFilter f = filter(1, 1, false);
        for (int i = 0; i < 5; i++) {
            assertThat(call(f, req("/actuator/health", "1.1.1.1")).getStatus()).isEqualTo(200);
            assertThat(call(f, req("/api/v10/x", "1.1.1.1")).getStatus()).isEqualTo(200);
        }
    }

    @Test
    void limitsPerUserIdAcrossIps() throws Exception {
        RateLimitFilter f = filter(100, 1, false);
        MockHttpServletRequest a = req("/api/v1/x", "1.1.1.1");
        a.addHeader("session-token", token("{\"uid\":\"42\"}"));
        MockHttpServletRequest b = req("/api/v1/x", "3.3.3.3");
        b.addHeader("session-token", token("{\"sub\":\"42\"}"));
        MockHttpServletRequest c = req("/api/v1/x", "3.3.3.3");
        c.addHeader("session-token", token("{\"uid\":\"43\"}"));
        assertThat(call(f, a).getStatus()).isEqualTo(200);
        assertThat(call(f, b).getStatus()).isEqualTo(429);
        assertThat(call(f, c).getStatus()).isEqualTo(200);
    }

    @Test
    void usesTheForwardedHeaderOnlyWhenTrusted() throws Exception {
        RateLimitFilter trusting = filter(1, 100, true);
        MockHttpServletRequest a = req("/api/v1/x", "9.9.9.9");
        a.addHeader("X-Forwarded-For", "5.5.5.5, 9.9.9.9");
        MockHttpServletRequest b = req("/api/v1/x", "9.9.9.9");
        b.addHeader("X-Forwarded-For", "6.6.6.6");
        assertThat(call(trusting, a).getStatus()).isEqualTo(200);
        assertThat(call(trusting, b).getStatus()).isEqualTo(200);
        assertThat(trusting.clientIp(req("/api/v1/x", "9.9.9.9"))).isEqualTo("9.9.9.9");

        RateLimitFilter plain = filter(1, 100, false);
        MockHttpServletRequest c = req("/api/v1/x", "9.9.9.9");
        c.addHeader("X-Forwarded-For", "5.5.5.5");
        MockHttpServletRequest d = req("/api/v1/x", "9.9.9.9");
        d.addHeader("X-Forwarded-For", "6.6.6.6");
        assertThat(call(plain, c).getStatus()).isEqualTo(200);
        assertThat(call(plain, d).getStatus()).isEqualTo(429);
    }

    @Test
    void userIdParsing() {
        assertThat(RateLimitFilter.userId(null)).isNull();
        assertThat(RateLimitFilter.userId("nodots")).isNull();
        assertThat(RateLimitFilter.userId("a.%%%.c")).isNull();
        assertThat(RateLimitFilter.userId(token("{\"x\":1}"))).isNull();
        assertThat(RateLimitFilter.userId(token("{\"uid\":7}"))).isEqualTo("7");
    }

    private static RateLimitFilter eventFilter(int events) {
        return new RateLimitFilter(new InMemoryRateLimitStore(new AtomicLong()::get), "/api/v1", 1000, 1000, false, events);
    }

    private static MockHttpServletRequest post(String uri, String ip) {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", uri);
        r.setRequestURI(uri);
        r.setRemoteAddr(ip);
        return r;
    }

    @Test
    void clickEventsHaveAStricterQuotaPerClientAndBusiness() throws Exception {
        RateLimitFilter f = eventFilter(2);
        assertThat(call(f, post("/api/v1/businesses/b1/events", "1.1.1.1")).getStatus()).isEqualTo(200);
        assertThat(call(f, post("/api/v1/businesses/b1/events", "1.1.1.1")).getStatus()).isEqualTo(200);
        MockHttpServletResponse res = call(f, post("/api/v1/businesses/b1/events", "1.1.1.1"));
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("X-RateLimit-Limit")).isEqualTo("2");
        assertThat(res.getHeader("Retry-After")).isNotNull();
        // another business, another client, and the owner-only summary are not affected
        assertThat(call(f, post("/api/v1/businesses/b2/events", "1.1.1.1")).getStatus()).isEqualTo(200);
        assertThat(call(f, post("/api/v1/businesses/b1/events", "2.2.2.2")).getStatus()).isEqualTo(200);
        assertThat(call(f, req("/api/v1/businesses/b1/events/summary", "1.1.1.1")).getStatus()).isEqualTo(200);
    }

    @Test
    void onlyPostsToTheEventsRouteUseTheEventBucket() throws Exception {
        RateLimitFilter f = eventFilter(1);
        for (int i = 0; i < 3; i++) {
            assertThat(call(f, req("/api/v1/businesses/b1/events", "1.1.1.1")).getStatus()).isEqualTo(200);
            assertThat(call(f, post("/api/v1/businesses/events", "1.1.1.1")).getStatus()).isEqualTo(200);
            assertThat(call(f, post("/api/v1/businesses/a/b/events", "1.1.1.1")).getStatus()).isEqualTo(200);
            assertThat(call(f, post("/api/v1/businesses/b1/views", "1.1.1.1")).getStatus()).isEqualTo(200);
            assertThat(call(f, post("/api/v1/other/b1/events", "1.1.1.1")).getStatus()).isEqualTo(200);
        }
    }

    @Test
    void theEventBucketIsOffWhenTheQuotaIsZeroAndTheDefaultConstructorKeepsItOff() throws Exception {
        RateLimitFilter zero = eventFilter(0);
        RateLimitFilter plain = filter(1000, 1000, false);
        for (int i = 0; i < 5; i++) {
            assertThat(call(zero, post("/api/v1/businesses/b1/events", "1.1.1.1")).getStatus()).isEqualTo(200);
            assertThat(call(plain, post("/api/v1/businesses/b1/events", "1.1.1.1")).getStatus()).isEqualTo(200);
        }
    }

    @Test
    void theGeneralIpQuotaStillAppliesFirstToEvents() throws Exception {
        RateLimitFilter f = new RateLimitFilter(new InMemoryRateLimitStore(new AtomicLong()::get), "/api/v1", 1, 1000, false, 50);
        assertThat(call(f, post("/api/v1/businesses/b1/events", "1.1.1.1")).getStatus()).isEqualTo(200);
        assertThat(call(f, post("/api/v1/businesses/b1/events", "1.1.1.1")).getStatus()).isEqualTo(429);
    }
}
