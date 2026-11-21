package com.umdc.directory.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Configuration
public class RedisConfig {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory(DataRedisProperties redisProperties) {
        Duration commandTimeout = redisProperties.getTimeout() != null
                ? redisProperties.getTimeout()
                : DEFAULT_TIMEOUT;
        Duration connectTimeout = redisProperties.getConnectTimeout() != null
                ? redisProperties.getConnectTimeout()
                : DEFAULT_TIMEOUT;

        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(connectTimeout).build())
                .build();

        RedisStandaloneConfiguration standaloneConfig;
        LettuceClientConfiguration clientConfig;

        String configuredUrl = redisProperties.getUrl();
        String host = redisProperties.getHost();
        boolean uriMode = looksLikeRedisUri(configuredUrl);

        if (uriMode) {
            RedisURI uri = RedisURI.create(configuredUrl);

            standaloneConfig = new RedisStandaloneConfiguration(uri.getHost(), uri.getPort());
            standaloneConfig.setDatabase(uri.getDatabase() >= 0 ? uri.getDatabase() : redisProperties.getDatabase());

            RedisCredentials credentials = uri.getCredentialsProvider().resolveCredentials().block();
            if (credentials != null) {
                if (credentials.hasUsername()) {
                    standaloneConfig.setUsername(credentials.getUsername());
                }
                if (credentials.hasPassword()) {
                    standaloneConfig.setPassword(RedisPassword.of(credentials.getPassword()));
                }
            }

            clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(commandTimeout)
                    .clientOptions(clientOptions)
                    .apply(uri)
                    .build();
        } else {
            String fallbackHost = StringUtils.hasText(configuredUrl) ? configuredUrl : host;
            standaloneConfig = new RedisStandaloneConfiguration(fallbackHost, redisProperties.getPort());
            standaloneConfig.setDatabase(redisProperties.getDatabase());
            if (StringUtils.hasText(redisProperties.getUsername())) {
                standaloneConfig.setUsername(redisProperties.getUsername());
            }
            if (StringUtils.hasText(redisProperties.getPassword())) {
                standaloneConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
            }

            LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder()
                    .commandTimeout(commandTimeout)
                    .clientOptions(clientOptions);

            if (redisProperties.getSsl() != null && redisProperties.getSsl().isEnabled()) {
                builder.useSsl();
            }

            clientConfig = builder.build();
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig, clientConfig);
        factory.setEagerInitialization(false);
        factory.setValidateConnection(true);
        return factory;
    }

    private static boolean looksLikeRedisUri(String value) {
        return StringUtils.hasText(value)
                && (value.startsWith("redis://") || value.startsWith("rediss://"));
    }
}
