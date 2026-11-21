package com.umdc.directory.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisConfigTest {

    private final RedisConfig config = new RedisConfig();

    @Test
    @DisplayName("host mode: host, port, database, credentials and timeout come from the properties")
    void hostMode() {
        DataRedisProperties props = new DataRedisProperties();
        props.setHost("redis.example.test");
        props.setPort(6380);
        props.setDatabase(2);
        props.setUsername("user");
        props.setPassword("pw");
        props.setTimeout(Duration.ofSeconds(3));

        LettuceConnectionFactory factory = config.redisConnectionFactory(props);

        RedisStandaloneConfiguration standalone = factory.getStandaloneConfiguration();
        assertEquals("redis.example.test", standalone.getHostName());
        assertEquals(6380, standalone.getPort());
        assertEquals(2, standalone.getDatabase());
        assertEquals("user", standalone.getUsername());
        assertTrue(standalone.getPassword().isPresent());
        assertEquals(Duration.ofSeconds(3), factory.getClientConfiguration().getCommandTimeout());
        assertFalse(factory.isUseSsl());
    }

    @Test
    @DisplayName("host mode: SSL enabled in the properties turns SSL on and defaults apply when no timeout is set")
    void hostModeWithSslAndDefaultTimeout() {
        DataRedisProperties props = new DataRedisProperties();
        props.setHost("redis.example.test");
        props.getSsl().setEnabled(true);
        props.setTimeout(null);
        props.setConnectTimeout(null);

        LettuceConnectionFactory factory = config.redisConnectionFactory(props);

        assertTrue(factory.isUseSsl());
        assertEquals(Duration.ofSeconds(10), factory.getClientConfiguration().getCommandTimeout());
        assertFalse(factory.getStandaloneConfiguration().getPassword().isPresent());
    }

    @Test
    @DisplayName("URI mode: redis:// URL provides host, port, database and credentials")
    void uriMode() {
        DataRedisProperties props = new DataRedisProperties();
        props.setUrl("redis://user:secret@uri.example.test:6390/4");

        LettuceConnectionFactory factory = config.redisConnectionFactory(props);

        RedisStandaloneConfiguration standalone = factory.getStandaloneConfiguration();
        assertEquals("uri.example.test", standalone.getHostName());
        assertEquals(6390, standalone.getPort());
        assertEquals(4, standalone.getDatabase());
        assertEquals("user", standalone.getUsername());
        assertTrue(standalone.getPassword().isPresent());
    }

    @Test
    @DisplayName("a bare host in the url property is used as host name")
    void bareHostInUrl() {
        DataRedisProperties props = new DataRedisProperties();
        props.setUrl("bare.example.test");

        LettuceConnectionFactory factory = config.redisConnectionFactory(props);

        assertEquals("bare.example.test", factory.getStandaloneConfiguration().getHostName());
    }
}
