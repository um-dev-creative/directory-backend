package com.prx.directory.client.backbone;

import com.prx.commons.general.pojo.Role;
import com.prx.directory.api.v1.to.PostProfileImageResponse;
import com.prx.directory.api.v1.to.PrxTokenString;
import com.prx.directory.client.backbone.to.*;
import com.prx.directory.client.interceptor.BackboneFeignConfigurer;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    /// @param backboneTokenRequest the authentication request containing user alias
    /// @return a PrxTokenString containing the authentication response with the session token
    /// @see ResponseEntity
    @PostMapping("/api/v1/session/token")
    PrxTokenString token(BackboneTokenRequest backboneTokenRequest);

    @PostMapping("/api/v1/users")
    BackboneUserCreateResponse post(BackboneUserCreateRequest backboneUserCreateRequest);

    @GetMapping("/api/v1/users/user/{userId}")
    BackboneUserGetResponse findUserById(@PathVariable UUID userId);

    @PutMapping(value = "/api/v1/users/{userId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> userPartialUpdate(@PathVariable UUID userId, @RequestBody BackboneUserUpdateRequest request);

    @PutMapping(value = "/api/v1/roles/find/{roleId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Role> findRoleById(@PathVariable UUID roleId);

    @DeleteMapping("/api/v1/users/application/{applicationId}/user/{userId}")
    ResponseEntity<Void> deleteUserByUserIdAndApplicationId(@PathVariable UUID applicationId, @PathVariable UUID userId);

    @PostMapping(value = "/api/v1/profile/image/application/{applicationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = {MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<PostProfileImageResponse> saveProfilePhoto(@RequestHeader(SESSION_TOKEN_KEY) String sessionToken, @PathVariable UUID applicationId, @RequestPart byte[] image);
}
