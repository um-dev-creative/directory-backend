package com.umdc.directory.client.interceptor;

import com.umdc.security.properties.AuthProperties;
import feign.RequestInterceptor;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;

public class MercuryFeignConfigurer extends BackendFeignConfigurer {

    public MercuryFeignConfigurer(AuthProperties authProperties, SslBundles sslBundles) {
        super(sslBundles, authProperties.getClients().stream()
                .filter(authProperties1 -> authProperties1.getId()
                        .equalsIgnoreCase(MERCURY_ID)).toList().getFirst());
    }

    @Bean("mercuryFeignClientInterceptor")
    @Override
    RequestInterceptor requestInterceptor() {
        return createInterceptor();
    }
}
