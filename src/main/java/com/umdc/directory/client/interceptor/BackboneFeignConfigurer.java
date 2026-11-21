package com.umdc.directory.client.interceptor;

import com.umdc.security.properties.AuthProperties;
import feign.RequestInterceptor;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;

public class BackboneFeignConfigurer extends BackendFeignConfigurer {

    public BackboneFeignConfigurer(AuthProperties authProperties, SslBundles sslBundles) {
        super(sslBundles, authProperties.getClients().stream()
                .filter(authProperties1 -> authProperties1.getId()
                        .equalsIgnoreCase(BACKBONE_ID)).findFirst().orElse(null));
    }

    @Bean("backboneFeignClientInterceptor")
    @Override
    RequestInterceptor requestInterceptor() {
        return createInterceptor();
    }
}
