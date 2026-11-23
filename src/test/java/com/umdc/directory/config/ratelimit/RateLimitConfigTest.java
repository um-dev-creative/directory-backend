package com.umdc.directory.config.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitConfigTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withUserConfiguration(RateLimitConfig.class)
            .withPropertyValues("umdc.api.endpoint=/api/v1");

    @Test
    void registersFilterAndInMemoryStoreWhenEnabled() {
        runner.withPropertyValues("umdc.ratelimit.enabled=true").run(ctx -> {
            assertThat(ctx.getBean(FilterRegistrationBean.class).getFilter()).isInstanceOf(RateLimitFilter.class);
        });
    }

    @Test
    void doesNothingWhenDisabledOrAbsent() {
        runner.withPropertyValues("umdc.ratelimit.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(FilterRegistrationBean.class));
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(FilterRegistrationBean.class));
    }

    @Test
    void aCustomStoreReplacesTheInMemoryOne() {
        runner.withPropertyValues("umdc.ratelimit.enabled=true")
                .withUserConfiguration(Custom.class)
                .run(ctx -> assertThat(ctx.getBean(FilterRegistrationBean.class).getFilter()).isInstanceOf(RateLimitFilter.class));
    }

    @Configuration
    static class Custom {
        @Bean
        RateLimitStore custom() {
            return (key, permits) -> new RateLimitDecision(true, permits, 0);
        }
    }
}
