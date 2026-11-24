package com.umdc.directory.config.ratelimit;

import com.umdc.directory.security.SessionTokenVerifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/// Wires the rate limit filter. Active only when `umdc.ratelimit.enabled=true` (default in `bootstrap.yml`: true; the test
/// profile turns it off).
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "umdc.ratelimit.enabled", havingValue = "true")
public class RateLimitConfig {

    /// @param storeProvider        a custom {@link RateLimitStore} (for example Redis) if declared, else the in-memory one
    /// @param apiPath              `umdc.api.endpoint`
    /// @param ipPerMinute          `umdc.ratelimit.ip-per-minute`
    /// @param userPerMinute        `umdc.ratelimit.user-per-minute`
    /// @param trustForwardedHeader `umdc.ratelimit.trust-forwarded-header`
    /// @param eventsPerMinute      `umdc.ratelimit.events-per-minute`: click events per IP and business (default 20)
    /// @param geocodePerMinute     `umdc.ratelimit.geocode-per-minute`: `POST /geocode` per user (else IP), default 10 (env `UMDC_RATELIMIT_GEOCODE_PER_MINUTE`)
    /// @param mapPerMinute         `umdc.ratelimit.map-per-minute`: `GET /businesses/map`, default 60 (env `UMDC_RATELIMIT_MAP_PER_MINUTE`)
    /// @param nearbyPerMinute      `umdc.ratelimit.nearby-per-minute`: `GET /businesses/nearby`, default 60 (env `UMDC_RATELIMIT_NEARBY_PER_MINUTE`)
    /// @param verifierProvider     verifies the session token before its uid picks a user bucket
    /// @return the filter registration, ahead of the Spring Security chain
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            ObjectProvider<RateLimitStore> storeProvider,
            @Value("${umdc.api.endpoint}") String apiPath,
            @Value("${umdc.ratelimit.ip-per-minute:120}") int ipPerMinute,
            @Value("${umdc.ratelimit.user-per-minute:300}") int userPerMinute,
            @Value("${umdc.ratelimit.trust-forwarded-header:false}") boolean trustForwardedHeader,
            @Value("${umdc.ratelimit.events-per-minute:20}") int eventsPerMinute,
            @Value("${umdc.ratelimit.geocode-per-minute:10}") int geocodePerMinute,
            @Value("${umdc.ratelimit.map-per-minute:60}") int mapPerMinute,
            @Value("${umdc.ratelimit.nearby-per-minute:60}") int nearbyPerMinute,
            ObjectProvider<SessionTokenVerifier> verifierProvider) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(
                new RateLimitFilter(storeProvider.getIfAvailable(InMemoryRateLimitStore::new), apiPath, ipPerMinute, userPerMinute, trustForwardedHeader, eventsPerMinute,
                        geocodePerMinute, mapPerMinute, nearbyPerMinute)
                        .withVerifier(verifierProvider.getIfAvailable()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
