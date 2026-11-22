package com.umdc.directory.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ResourceBanner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The versions in the startup banner come from a Maven-filtered file: an unfiltered {@code @...@} would print as-is. */
class BannerVersionsTest {

    private static final String[] KEYS = {"umdc.build.spring-cloud-version", "umdc.build.umdc-libs-version"};

    private Properties filteredVersions() throws IOException {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("build-versions.properties")) {
            assertNotNull(in, "build-versions.properties must be on the classpath");
            props.load(in);
        }
        return props;
    }

    @Test
    @DisplayName("the filtered file has real versions, not @...@ placeholders")
    void theFilteredFileHasRealVersions() throws IOException {
        Properties props = filteredVersions();
        for (String key : KEYS) {
            String value = props.getProperty(key);
            assertTrue(value != null && !value.isBlank(), key + " is empty");
            assertFalse(value.contains("@"), key + " was not filtered: " + value);
        }
    }

    @Test
    @DisplayName("the application imports the filtered file and the banner prints both versions")
    void theBannerAndTheConfigurationUseTheFile() throws IOException {
        String banner = Files.readString(Path.of("src/main/resources/banner.txt"));
        assertTrue(banner.contains("${umdc.build.spring-cloud-version:n/a}"));
        assertTrue(banner.contains("${umdc.build.umdc-libs-version:n/a}"));
        assertTrue(Files.readString(Path.of("src/main/resources/bootstrap.yml")).contains("optional:classpath:build-versions.properties"));
    }

    @Test
    @DisplayName("the banner renders with every placeholder resolved")
    void theBannerRendersWithoutPlaceholders() throws IOException {
        Properties versions = filteredVersions();
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "umdc.app.title", "Directory Backend Service",
                "umdc.app.version", "0.0.1",
                KEYS[0], versions.getProperty(KEYS[0]),
                KEYS[1], versions.getProperty(KEYS[1]))));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ResourceBanner(new ClassPathResource("banner.txt")).printBanner(env, getClass(), new PrintStream(out, true, StandardCharsets.UTF_8));
        String printed = out.toString(StandardCharsets.UTF_8);

        assertTrue(printed.contains("Directory Backend Service::0.0.1"));
        assertTrue(printed.contains("Spring Cloud " + versions.getProperty(KEYS[0])));
        assertTrue(printed.contains("UMDC libraries " + versions.getProperty(KEYS[1])));
        assertFalse(printed.contains("${"), "unresolved placeholder in the banner");
        assertFalse(printed.contains("n/a"), "a version fell back to n/a");
    }
}
