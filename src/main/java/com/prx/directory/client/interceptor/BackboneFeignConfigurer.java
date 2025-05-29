package com.prx.directory.client.interceptor;

import com.prx.security.properties.AuthProperties;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

public class BackboneFeignConfigurer extends BackendFeignConfigurer {

    public BackboneFeignConfigurer(AuthProperties authProperties) {
        super(authProperties.getClients().stream()
                .filter(authProperties1 -> authProperties1.getId()
                        .equalsIgnoreCase(BACKBONE_ID)).findFirst().orElse(null));
    }

    @Bean("backboneFeignClientInterceptor")
    @Override
    RequestInterceptor requestInterceptor() {
        return createInterceptor();
    }
}
