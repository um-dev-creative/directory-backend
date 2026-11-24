package com.umdc.directory.client.mercury;

import com.umdc.directory.api.v1.to.PrxTokenString;
import com.umdc.directory.client.interceptor.MercuryAuthFeignConfig;
import com.umdc.security.to.AuthRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/// Feign client of Mercury's token endpoint (MER-11). The configuration only sets timeouts: no OAuth Bearer is added here.
@FeignClient(name = "mercuryAuthClient", url = "https://prx-qa.backbone.tst/mercury", configuration = MercuryAuthFeignConfig.class)
public interface MercuryAuthClient {

    /// Logs Directory in to Mercury with its registered alias and password alone (Mercury's `umdc.mercury.login-clients`). Wrong
    /// credentials are throttled by Mercury (about 5 failures in 15 minutes, then 429 with `Retry-After`).
    ///
    /// @param request the alias and password of Directory's login client
    /// @return Mercury's token; it carries no user and is only good for `/api/v1/verification-code` and `/api/v1/mail`
    @PostMapping("/api/v1/auth/token")
    PrxTokenString token(@RequestBody AuthRequest request);
}
