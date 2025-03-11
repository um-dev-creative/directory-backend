package com.prx.directory.client.backbone;

import com.prx.directory.api.v1.to.PrxTokenString;
import com.prx.directory.client.backbone.to.BackboneTokenRequest;
import com.prx.directory.client.backbone.to.BackboneUserCreateRequest;
import com.prx.directory.client.backbone.to.BackboneUserCreateResponse;
import com.prx.directory.client.backbone.to.BackboneUserGetResponse;
import com.prx.directory.client.interceptor.BackboneFeignConfigurer;
import com.prx.security.to.AuthRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

import static com.prx.security.constant.ConstantApp.SESSION_TOKEN_KEY;

@FeignClient(name = "backboneClient", url = "https://prx-qa.backbone.tst/backbone", configuration = BackboneFeignConfigurer.class)
public interface BackboneClient {

    @GetMapping("/api/v1/session/validate")
    boolean validate(@RequestHeader(SESSION_TOKEN_KEY) String sessionToken);

    @GetMapping("/api/v1/users/check/alias/{alias}/application/{applicationId}")
    ResponseEntity<Void> checkAlias(@PathVariable String alias, @PathVariable UUID applicationId);

    @GetMapping("/api/v1/users/check/email/{email}/application/{applicationId}")
    ResponseEntity<Void> checkEmail(@PathVariable String email, @PathVariable UUID applicationId);

    ///  Generates a session token based on the provided authentication request.
    ///
    /// @param authRequest the authentication request containing user alias
    /// @return a PrxTokenString containing the authentication response with the session token
    ///
    /// @see AuthRequest
    /// @see ResponseEntity
    @PostMapping("/api/v1/session/token")
    PrxTokenString token(BackboneTokenRequest authRequest);

    @PostMapping("/api/v1/users")
    BackboneUserCreateResponse post(BackboneUserCreateRequest backboneUserCreateRequest);

    @GetMapping("/api/v1/users/user/{userId}")
    BackboneUserGetResponse find(@PathVariable UUID userId);
}
