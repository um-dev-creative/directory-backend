package com.umdc.directory.client.interceptor;

import feign.Request;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/// Feign settings of the Mercury token exchange: only timeouts. No interceptor on purpose: the exchange is the call that gets the token,
/// so it must not carry the OAuth Bearer that [MercuryFeignConfigurer] adds to the other Mercury calls.
public class MercuryAuthFeignConfig {

    /// @return connect 3 s, read 5 s, follow redirects
    @Bean("mercuryAuthRequestOptions")
    public Request.Options mercuryAuthRequestOptions() {
        return new Request.Options(3, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }
}
