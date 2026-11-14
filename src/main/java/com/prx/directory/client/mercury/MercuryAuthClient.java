package com.prx.directory.client.mercury;

import com.prx.directory.api.v1.to.PrxTokenString;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/// Feign client of Mercury's token exchange. No `configuration`: the `Authorization` header here is the Backbone M2M token passed by
/// the caller, so the OAuth Bearer that [MercuryClient] adds to its calls must not be added as well.
@FeignClient(name = "mercuryAuthClient", url = "https://prx-qa.backbone.tst/mercury")
public interface MercuryAuthClient {

    /// Trades Directory's Backbone M2M token for Mercury's own M2M token.
    ///
    /// @param bearer `Bearer <Backbone M2M token>`
    /// @return Mercury's token, to be sent in `session-token`
    @PostMapping("/api/v1/auth/token")
    PrxTokenString exchange(@RequestHeader(AUTHORIZATION) String bearer);
}
