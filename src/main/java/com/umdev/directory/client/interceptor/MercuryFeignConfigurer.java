package com.umdev.directory.client.interceptor;

import com.umdev.security.properties.AuthProperties;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

public class MercuryFeignConfigurer extends BackendFeignConfigurer {

    public MercuryFeignConfigurer(AuthProperties authProperties) {
        super(authProperties.getClients().stream()
                .filter(authProperties1 -> authProperties1.getId()
                        .equalsIgnoreCase(MERCURY_ID)).toList().getFirst());
    }

    @Bean("mercuryFeignClientInterceptor")
    @Override
    RequestInterceptor requestInterceptor() {
        return createInterceptor();
    }
}
