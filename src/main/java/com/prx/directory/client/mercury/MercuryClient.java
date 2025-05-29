package com.prx.directory.client.mercury;

import com.prx.directory.api.v1.to.PrxTokenString;
import com.prx.directory.client.interceptor.MercuryFeignConfigurer;
import com.prx.directory.client.mercury.to.VerificationCodeRequest;
import com.prx.security.to.AuthRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static com.prx.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/// Feign client for the Mercury service. This client is used to interact with the Mercury service.
@FeignClient(name = "mercuryClient", url = "https://prx-qa.backbone.tst/mercury", configuration = {MercuryFeignConfigurer.class})
public interface MercuryClient {
    String SESSION_TOKEN_BKD_KEY = "session-token-bkd";

    /// Sends a verification code to the specified phone number.
    ///
    /// @param sessionToken the session token used to authenticate the request
    /// @param verificationCodeRequest the request object containing the phone number
    /// @return a ResponseEntity containing the response of the verification code operation
    @PostMapping("/api/v1/verification-code")
    ResponseEntity<Void> confirmCode(@RequestHeader(SESSION_TOKEN_KEY) String sessionToken, VerificationCodeRequest verificationCodeRequest);

    /// Generates a session token based on the provided authentication request.
    ///
    /// @param sessionToken the session token used to authenticate the request
    /// @param authRequest the authentication request containing user alias
    /// @return a PrxTokenString containing the authentication response with the session token
    ///
    /// @see AuthRequest
    /// @see ResponseEntity
    @PostMapping("/api/v1/auth/token")
    PrxTokenString token(@RequestHeader(SESSION_TOKEN_BKD_KEY) String sessionToken, AuthRequest authRequest);

}
