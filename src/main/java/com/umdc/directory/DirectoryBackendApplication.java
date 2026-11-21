package com.umdc.directory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/// Entry point. It scans the same packages as `@SpringBootApplication(scanBasePackages = ...)` did, with one exclusion: the security
/// library's `SecurityConfig`, the decommissioned Keycloak-style JWT resource-server chain (it needs a `jwk-set-uri` and answers 401 to
/// the `session-token` header every client sends). smartkin-backend, mercury and zerobound-rest exclude it the same way;
/// `SessionTokenSecurityConfig` and the library's `SessionJwtInterceptor` authenticate instead.
@EnableFeignClients(basePackages = "com.umdc.directory.client")
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {
                "com.umdc.commons.services",
                "com.umdc.directory",
                "com.umdc.security"
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.umdc\\.security\\.config\\.SecurityConfig")
        }
)
public class DirectoryBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(DirectoryBackendApplication.class, args);
    }
}
