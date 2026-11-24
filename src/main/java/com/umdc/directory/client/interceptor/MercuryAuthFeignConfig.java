package com.umdc.directory.client.interceptor;

import feign.Request;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/// Feign settings of the two token calls that make up the Mercury M2M exchange (Backbone managed-client token and Mercury's `/auth/token`): only timeouts. No interceptor on purpose: these calls get the tokens, so they must
/// not carry the OAuth Bearer that [MercuryFeignConfigurer] and [BackboneFeignConfigurer] add to the other calls.
public class MercuryAuthFeignConfig {

    /// @return connect 3 s, read 5 s, follow redirects
    @Bean("mercuryAuthRequestOptions")
    public Request.Options mercuryAuthRequestOptions() {
        return new Request.Options(3, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }
}
