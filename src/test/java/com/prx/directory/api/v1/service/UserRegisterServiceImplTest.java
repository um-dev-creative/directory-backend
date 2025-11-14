package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.ConfirmCodeRequest;
import com.prx.directory.client.backbone.BackboneClient;
import com.prx.directory.client.backbone.to.BackboneUserGetResponse;
import com.prx.directory.client.mercury.MercuryClient;
import com.prx.directory.client.mercury.to.VerificationCodeRequest;
import com.prx.directory.mapper.ConfirmCodeMapper;
import com.prx.directory.api.v1.to.PrxTokenString;
import com.prx.security.to.AuthRequest;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserRegisterServiceImplTest {

    MercuryClient mercuryClient;
    ConfirmCodeMapper confirmCodeMapper;
    BackboneClient backboneClient;
    UserRegisterServiceImpl service;

    @BeforeEach
    void setup() {
        mercuryClient = mock(MercuryClient.class);
        confirmCodeMapper = mock(ConfirmCodeMapper.class);
        backboneClient = mock(BackboneClient.class);
        service = new UserRegisterServiceImpl(mercuryClient, confirmCodeMapper, backboneClient);
        ReflectionTestUtils.setField(service, "applicationId", UUID.randomUUID());
    }

    @Test
    void confirmCode_success() {
        UUID userId = UUID.randomUUID();
        var userResp = new BackboneUserGetResponse(userId, "alias","pass","email","disp", LocalDateTime.now(), LocalDateTime.now(),true,true,true,true,null, List.of(), List.of());
        when(backboneClient.findUserById(userId)).thenReturn(userResp);
        when(mercuryClient.token(anyString(), any(AuthRequest.class))).thenReturn(new PrxTokenString("mercury-token"));
        when(confirmCodeMapper.toVerificationCodeRequest(any(ConfirmCodeRequest.class), any(UUID.class))).then(inv -> {
            ConfirmCodeRequest c = inv.getArgument(0);
            UUID appId = inv.getArgument(1);
            return new VerificationCodeRequest(appId, c.userId(), c.verificationCode());
        });
        when(mercuryClient.confirmCode(anyString(), any(VerificationCodeRequest.class))).thenReturn(ResponseEntity.ok().build());

        var resp = service.confirmCode("bkd", new ConfirmCodeRequest(userId, "123456789"));
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void confirmCode_feignErrorPropagatesStatusAndHeader() {
        UUID userId = UUID.randomUUID();
        var userResp = new BackboneUserGetResponse(userId, "alias","pass","email","disp", LocalDateTime.now(), LocalDateTime.now(),true,true,true,true,null, List.of(), List.of());
        when(backboneClient.findUserById(userId)).thenReturn(userResp);
        when(mercuryClient.token(anyString(), any(AuthRequest.class))).thenReturn(new PrxTokenString("t"));
        when(confirmCodeMapper.toVerificationCodeRequest(any(), any())).thenAnswer(inv -> new VerificationCodeRequest(inv.getArgument(1), userId, "123456789"));

        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("message", List.of("error-message"));
        FeignException ex = new FeignException.BadRequest("bad", mockRequest(), new byte[0], headers);
        when(mercuryClient.confirmCode(anyString(), any(VerificationCodeRequest.class))).thenThrow(ex);

        var resp = service.confirmCode("bkd", new ConfirmCodeRequest(userId, "123456789"));
        assertEquals(400, resp.getStatusCode().value());
        assertTrue(resp.getHeaders().containsKey("message"));
    }

    private static feign.Request mockRequest() {
        return feign.Request.create(feign.Request.HttpMethod.GET, "http://x", Map.of(), null, StandardCharsets.UTF_8, null);
    }
}
