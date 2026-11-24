package com.umdc.directory.config;

import com.umdc.directory.constant.DirectoryAppConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/// CORS for the API (`umdc.api.endpoint` + `/**`), driven by `umdc.cors.allowed-origins` (env `UMDC_CORS_ALLOWED_ORIGINS`, comma list).
/// Empty (default) means no cross-origin browser request is allowed (it is answered `403`); same-origin requests and non-browser
/// clients are unaffected. A wildcard origin is refused at startup. Credentials are not allowed (the session token travels in a header).
@Configuration(proxyBeanMethods = false)
public class CorsConfig {

    /// Response headers a browser script may read.
    static final List<String> EXPOSED_HEADERS = List.of(DirectoryAppConstants.MESSAGE_HEADER, DirectoryAppConstants.MESSAGE_ERROR_HEADER, "Retry-After", "X-RateLimit-Limit");

    /// @param apiPath        `umdc.api.endpoint`
    /// @param allowedOrigins `umdc.cors.allowed-origins`: comma separated exact origins, e.g. `https://app.example.com`
    /// @param allowedMethods `umdc.cors.allowed-methods`
    /// @param allowedHeaders `umdc.cors.allowed-headers`
    /// @param maxAgeSeconds  `umdc.cors.max-age-seconds`: how long a browser may cache a preflight answer
    /// @return the filter registration, ahead of the rate limiter and the Spring Security chain
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(
            @Value("${umdc.api.endpoint}") String apiPath,
            @Value("${umdc.cors.allowed-origins:}") String allowedOrigins,
            @Value("${umdc.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}") String allowedMethods,
            @Value("${umdc.cors.allowed-headers:session-token,Content-Type,Accept,Authorization}") String allowedHeaders,
            @Value("${umdc.cors.max-age-seconds:1800}") long maxAgeSeconds) {
        var configuration = new CorsConfiguration();
        var origins = split(allowedOrigins);
        if (origins.contains("*")) {
            throw new IllegalStateException("umdc.cors.allowed-origins must list exact origins; '*' is not accepted");
        }
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(split(allowedMethods));
        configuration.setAllowedHeaders(split(allowedHeaders));
        configuration.setExposedHeaders(EXPOSED_HEADERS);
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(maxAgeSeconds);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(apiPath + "/**", configuration);
        var registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        return registration;
    }

    private static List<String> split(String csv) {
        return Arrays.stream(csv.split(",")).map(String::trim).filter(v -> !v.isEmpty()).toList();
    }
}
