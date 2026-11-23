package com.umdc.directory.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/// Contract of the platform defaults in `bootstrap.yml` (production values): documentation closed, rate limiting on,
/// Flyway off with the V5 baseline.
class PlatformDefaultsTest {

    private static Properties bootstrap() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("bootstrap.yml"));
        // the dev profile document is not selected: only the first (default) document and the shared ones apply
        yaml.setDocumentMatchers(properties -> {
            String profile = properties.getProperty("spring.config.activate.on-profile");
            return profile == null
                    ? org.springframework.beans.factory.config.YamlProcessor.MatchStatus.FOUND
                    : org.springframework.beans.factory.config.YamlProcessor.MatchStatus.NOT_FOUND;
        });
        return yaml.getObject();
    }

    @Test
    void flywayIsOffByDefaultWithTheV5Baseline() {
        Properties p = bootstrap();
        assertThat(p.getProperty("spring.flyway.enabled")).isEqualTo("${SPRING_FLYWAY_ENABLED:false}");
        assertThat(p.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo("true");
        assertThat(p.getProperty("spring.flyway.baseline-version")).isEqualTo("5");
        assertThat(p.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration");
    }

    @Test
    void documentationIsClosedAndRateLimitingOnByDefault() {
        Properties p = bootstrap();
        assertThat(p.getProperty("umdc.docs.enabled")).isEqualTo("${UMDC_DOCS_ENABLED:false}");
        assertThat(p.getProperty("springdoc.api-docs.enabled")).isEqualTo("${umdc.docs.enabled}");
        assertThat(p.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("${umdc.docs.enabled}");
        assertThat(p.getProperty("umdc.ratelimit.enabled")).isEqualTo("${UMDC_RATELIMIT_ENABLED:true}");
        assertThat(p.getProperty("umdc.ratelimit.ip-per-minute")).isEqualTo("${UMDC_RATELIMIT_IP_PER_MINUTE:120}");
        assertThat(p.getProperty("umdc.ratelimit.user-per-minute")).isEqualTo("${UMDC_RATELIMIT_USER_PER_MINUTE:300}");
    }
}
