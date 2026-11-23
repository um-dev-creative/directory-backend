package com.umdc.directory.client.geocoding;

import feign.Request;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/// Per-client Feign configuration (deliberately not annotated: it must not apply to the other clients).
public class GeocodingFeignConfiguration {

    public GeocodingFeignConfiguration() {
        // instantiated by Spring Cloud OpenFeign for the geocoding client only
    }

    @Bean
    Request.Options geocodingRequestOptions(GeocodingProperties properties) {
        return new Request.Options(Duration.ofMillis(properties.getConnectTimeoutMs()),
                Duration.ofMillis(properties.getReadTimeoutMs()), true);
    }
}
