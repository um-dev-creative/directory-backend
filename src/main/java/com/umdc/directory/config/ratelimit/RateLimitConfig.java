package com.umdc.directory.config.ratelimit;

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
    /// @return the filter registration, ahead of the Spring Security chain
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            ObjectProvider<RateLimitStore> storeProvider,
            @Value("${umdc.api.endpoint}") String apiPath,
            @Value("${umdc.ratelimit.ip-per-minute:120}") int ipPerMinute,
            @Value("${umdc.ratelimit.user-per-minute:300}") int userPerMinute,
            @Value("${umdc.ratelimit.trust-forwarded-header:false}") boolean trustForwardedHeader,
            @Value("${umdc.ratelimit.events-per-minute:20}") int eventsPerMinute) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(
                new RateLimitFilter(storeProvider.getIfAvailable(InMemoryRateLimitStore::new), apiPath, ipPerMinute, userPerMinute, trustForwardedHeader, eventsPerMinute));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
