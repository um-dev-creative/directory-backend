package com.umdc.directory.i18n;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/// Registers {@link I18nFilter} ahead of the rate limiter (order `HIGHEST_PRECEDENCE + 10`) and of Spring Security.
@Configuration
public class I18nConfig {

    /// Default constructor.
    public I18nConfig() {
        // no state
    }

    /// @param translator message translator
    /// @return the filter registration
    @Bean
    public FilterRegistrationBean<I18nFilter> i18nFilterRegistration(MessageTranslator translator) {
        FilterRegistrationBean<I18nFilter> registration = new FilterRegistrationBean<>(new I18nFilter(translator));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        return registration;
    }
}
