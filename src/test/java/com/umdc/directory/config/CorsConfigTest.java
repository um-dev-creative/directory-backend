package com.umdc.directory.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withUserConfiguration(CorsConfig.class)
            .withPropertyValues("umdc.api.endpoint=/api/v1");

    private static MockHttpServletResponse call(CorsFilter f, String method, String uri, String origin, String reqMethod) throws Exception {
        var req = new MockHttpServletRequest(method, uri);
        req.setRequestURI(uri);
        req.setServerName("api.example.com");
        req.addHeader("Origin", origin);
        if (reqMethod != null) {
            req.addHeader("Access-Control-Request-Method", reqMethod);
            req.addHeader("Access-Control-Request-Headers", "session-token");
        }
        var res = new MockHttpServletResponse();
        f.doFilter(req, res, new MockFilterChain());
        return res;
    }

    @SuppressWarnings("unchecked")
    private static CorsFilter filter(org.springframework.context.ApplicationContext ctx) {
        return ((FilterRegistrationBean<CorsFilter>) ctx.getBean(FilterRegistrationBean.class)).getFilter();
    }

    @Test
    void emptyOriginsAllowNoCrossOriginRequest() {
        runner.run(ctx -> {
            var res = call(filter(ctx), "GET", "/api/v1/businesses", "https://evil.example", null);
            assertThat(res.getStatus()).isEqualTo(403);
            assertThat(res.getHeader("Access-Control-Allow-Origin")).isNull();
        });
    }

    @Test
    void listedOriginIsAllowedWithExposedHeaders() {
        runner.withPropertyValues("umdc.cors.allowed-origins= https://app.example.com , https://admin.example.com").run(ctx -> {
            var res = call(filter(ctx), "GET", "/api/v1/businesses", "https://app.example.com", null);
            assertThat(res.getStatus()).isEqualTo(200);
            assertThat(res.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://app.example.com");
            assertThat(res.getHeader("Access-Control-Expose-Headers")).contains("message-error", "Retry-After", "message");
            assertThat(res.getHeader("Access-Control-Allow-Credentials")).isNull();
            assertThat(call(filter(ctx), "GET", "/api/v1/x", "https://admin.example.com", null).getStatus()).isEqualTo(200);
            assertThat(call(filter(ctx), "GET", "/api/v1/x", "https://evil.example", null).getStatus()).isEqualTo(403);
        });
    }

    @Test
    void preflightHonoursTheMethodAndHeaderAllowLists() {
        runner.withPropertyValues("umdc.cors.allowed-origins=https://app.example.com").run(ctx -> {
            var ok = call(filter(ctx), "OPTIONS", "/api/v1/businesses", "https://app.example.com", "PATCH");
            assertThat(ok.getStatus()).isEqualTo(200);
            assertThat(ok.getHeader("Access-Control-Allow-Methods")).contains("PATCH");
            assertThat(ok.getHeader("Access-Control-Max-Age")).isEqualTo("1800");
            var bad = call(filter(ctx), "OPTIONS", "/api/v1/businesses", "https://app.example.com", "TRACE");
            assertThat(bad.getStatus()).isEqualTo(403);
        });
    }

    @Test
    void methodsHeadersAndMaxAgeAreConfigurable() {
        runner.withPropertyValues("umdc.cors.allowed-origins=https://app.example.com", "umdc.cors.allowed-methods=GET",
                "umdc.cors.allowed-headers=session-token", "umdc.cors.max-age-seconds=60").run(ctx -> {
            assertThat(call(filter(ctx), "OPTIONS", "/api/v1/x", "https://app.example.com", "POST").getStatus()).isEqualTo(403);
            var ok = call(filter(ctx), "OPTIONS", "/api/v1/x", "https://app.example.com", "GET");
            assertThat(ok.getHeader("Access-Control-Max-Age")).isEqualTo("60");
        });
    }

    @Test
    void aWildcardOriginFailsAtStartup() {
        runner.withPropertyValues("umdc.cors.allowed-origins=*").run(ctx ->
                assertThat(ctx).hasFailed().getFailure().hasStackTraceContaining("exact origins"));
    }

    @Test
    void doesNotApplyOutsideTheApiPath() {
        runner.run(ctx -> assertThat(call(filter(ctx), "GET", "/actuator/health", "https://evil.example", null).getStatus()).isEqualTo(200));
    }
}
