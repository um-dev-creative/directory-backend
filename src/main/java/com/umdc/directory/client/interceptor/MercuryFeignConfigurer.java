package com.umdc.directory.client.interceptor;

import com.umdc.security.properties.AuthProperties;
import feign.Request;
import feign.RequestInterceptor;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

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

    /// Timeouts of every Mercury call: connect 3 s, read 5 s, so a slow Mercury cannot hold the request threads.
    ///
    /// @return the Feign request options
    @Bean("mercuryRequestOptions")
    public Request.Options mercuryRequestOptions() {
        return new Request.Options(3, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }
}
