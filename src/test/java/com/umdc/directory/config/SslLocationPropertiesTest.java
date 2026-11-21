package com.umdc.directory.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The TLS bundle, the Vault / Config Server / Eureka clients and the inter-service RestTemplate read the keystores from the classpath
/// by default (the jar carries them, see pom.xml) and from an explicit URL when `SSL_KEYSTORE_URL` / `SSL_TRUSTSTORE_URL` is set.
class SslLocationPropertiesTest {

    private static final String[] KEYSTORE = {"spring.ssl.bundle.jks.directory-security.keystore.location"};
    private static final String[] TRUSTSTORES = {"spring.ssl.bundle.jks.directory-security.truststore.location",
            "umdc.security.truststore.location", "spring.cloud.vault.ssl.trust-store", "spring.cloud.config.tls.trust-store",
            "eureka.client.tls.trust-store"};

    private static PropertySourcesPropertyResolver resolver(Map<String, Object> env) throws IOException {
        var sources = new MutablePropertySources();
        sources.addFirst(new MapPropertySource("env", env));
        for (PropertySource<?> source : new YamlPropertySourceLoader().load("bootstrap", new ClassPathResource("bootstrap.yml"))) {
            sources.addLast(source);
        }
        return new PropertySourcesPropertyResolver(sources);
    }

    @Test
    void defaultToTheClasspathNames() throws IOException {
        var resolver = resolver(Map.of("SSL_KEYSTORE_LOCATION", "directory.jks", "SSL_TRUSTSTORE_LOCATION", "umdc-truststore.jks"));
        for (String key : KEYSTORE) {
            assertEquals("classpath:directory.jks", resolver.getProperty(key), key);
        }
        for (String key : TRUSTSTORES) {
            assertEquals("classpath:umdc-truststore.jks", resolver.getProperty(key), key);
        }
    }

    @Test
    void anExplicitUrlWins() throws IOException {
        var resolver = resolver(Map.of("SSL_KEYSTORE_LOCATION", "directory.jks", "SSL_TRUSTSTORE_LOCATION", "umdc-truststore.jks",
                "SSL_KEYSTORE_URL", "file:/certs/directory.jks", "SSL_TRUSTSTORE_URL", "file:/certs/umdc-truststore.jks"));
        for (String key : KEYSTORE) {
            assertEquals("file:/certs/directory.jks", resolver.getProperty(key), key);
        }
        for (String key : TRUSTSTORES) {
            assertEquals("file:/certs/umdc-truststore.jks", resolver.getProperty(key), key);
        }
    }

    @Test
    void theSessionInterceptorProtectsTheApiPath() throws IOException {
        assertEquals("/api/v1", resolver(Map.of()).getProperty("umdc.api.endpoint"));
    }
}
