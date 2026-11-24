package com.umdc.directory.client.mercury;

import com.umdc.directory.client.interceptor.MercuryFeignConfigurer;
import com.umdc.directory.client.mercury.to.VerificationCodeRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/// Feign client for the Mercury service. Every call here carries Directory's Mercury token in `session-token`; get it from
/// {@link MercuryTokens} (the Backbone M2M exchange itself is {@link MercuryAuthClient}). That token has no user: it is only good for
/// `/api/v1/verification-code` and `/api/v1/mail`. A user-scoped Mercury call (campaigns, templates) must instead send
/// `Authorization: Bearer <Backbone opaque token>` (added by [MercuryFeignConfigurer]) and the end user's own backbone `session-token`,
/// passed through unchanged; Mercury refuses a refresh token or a logged-out token there (401). Timeouts: 3 s connect, 5 s read.
@FeignClient(name = "mercuryClient", url = "https://prx-qa.backbone.tst/mercury", configuration = {MercuryFeignConfigurer.class})
public interface MercuryClient {
    /// Sends a verification code to the specified phoneNumber number.
    ///
    /// @param sessionToken the session token used to authenticate the request
    /// @param verificationCodeRequest the request object containing the phoneNumber number
    /// @return a ResponseEntity containing the response of the verification code operation
    @PostMapping("/api/v1/verification-code")
    ResponseEntity<Void> confirmCode(@RequestHeader(SESSION_TOKEN_KEY) String sessionToken, VerificationCodeRequest verificationCodeRequest);

    /**
     * Checks the latest verification code status for a user.
     *
     * @param sessionToken the session token used to authenticate the request
     * @param userId the user id (UUID) to check status for
     * @return true if the verification code is done, false if pending
     */
    @GetMapping("/api/v1/verification-code/latest-status")
    Boolean isVerificationCodeDone(@RequestHeader(SESSION_TOKEN_KEY) String sessionToken, @RequestParam("userId") UUID userId);

}
