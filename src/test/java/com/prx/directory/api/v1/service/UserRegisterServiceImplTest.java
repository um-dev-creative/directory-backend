package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.ConfirmCodeRequest;
import com.prx.directory.client.mercury.MercuryClient;
import com.prx.directory.client.mercury.MercuryTokens;
import com.prx.directory.client.mercury.to.VerificationCodeRequest;
import com.prx.directory.mapper.ConfirmCodeMapper;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserRegisterServiceImplTest {

    MercuryClient mercuryClient;
    ConfirmCodeMapper confirmCodeMapper;
    MercuryTokens mercuryTokens;
    UserRegisterServiceImpl service;

    @BeforeEach
    void setup() {
        mercuryClient = mock(MercuryClient.class);
        confirmCodeMapper = mock(ConfirmCodeMapper.class);
        mercuryTokens = mock(MercuryTokens.class);
        service = new UserRegisterServiceImpl(mercuryClient, mercuryTokens, confirmCodeMapper);
        ReflectionTestUtils.setField(service, "applicationId", UUID.randomUUID());
    }

    @SuppressWarnings("unchecked")
    private void runsCallsWithTheMercuryToken() {
        when(mercuryTokens.withToken(any())).thenAnswer(inv -> ((Function<String, Object>) inv.getArgument(0)).apply("mercury-token"));
    }

    @Test
    @DisplayName("confirmCode: success returns 200")
    void confirmCode_success() {
        UUID userId = UUID.randomUUID();
        runsCallsWithTheMercuryToken();
        when(confirmCodeMapper.toVerificationCodeRequest(any(ConfirmCodeRequest.class), any(UUID.class))).then(inv -> {
            ConfirmCodeRequest c = inv.getArgument(0);
            UUID appId = inv.getArgument(1);
            return new VerificationCodeRequest(appId, c.userId(), c.verificationCode());
        });
        when(mercuryClient.confirmCode(anyString(), any(VerificationCodeRequest.class))).thenReturn(ResponseEntity.ok().build());

        var resp = service.confirmCode("bkd", new ConfirmCodeRequest(userId, "123456789"));
        assertEquals(200, resp.getStatusCode().value());
        verify(mercuryClient).confirmCode(eq("mercury-token"), any(VerificationCodeRequest.class));
    }

    @Test
    @DisplayName("confirmCode: Feign error propagates status and headers")
    void confirmCode_feignErrorPropagatesStatusAndHeader() {
        UUID userId = UUID.randomUUID();
        runsCallsWithTheMercuryToken();
        when(confirmCodeMapper.toVerificationCodeRequest(any(), any())).thenAnswer(inv -> new VerificationCodeRequest(inv.getArgument(1), userId, "123456789"));

        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("message", List.of("error-message"));
        FeignException ex = new FeignException.BadRequest("bad", mockRequest(), new byte[0], headers);
        when(mercuryClient.confirmCode(anyString(), any(VerificationCodeRequest.class))).thenThrow(ex);

        var resp = service.confirmCode("bkd", new ConfirmCodeRequest(userId, "123456789"));
        assertEquals(400, resp.getStatusCode().value());
        assertTrue(resp.getHeaders().containsKey("message"));
    }

    @Test
    @DisplayName("confirmCode: Feign error without message header returns status and empty header")
    void confirmCode_feignErrorWithoutMessageHeader() {
        UUID userId = UUID.randomUUID();
        runsCallsWithTheMercuryToken();
        when(confirmCodeMapper.toVerificationCodeRequest(any(), any())).thenAnswer(inv -> new VerificationCodeRequest(inv.getArgument(1), userId, "123456789"));

        FeignException ex = new FeignException.BadRequest("bad", mockRequest(), new byte[0], new HashMap<>());
        when(mercuryClient.confirmCode(anyString(), any(VerificationCodeRequest.class))).thenThrow(ex);

        var resp = service.confirmCode("bkd", new ConfirmCodeRequest(userId, "123456789"));
        assertEquals(400, resp.getStatusCode().value());
    }

    private static feign.Request mockRequest() {
        return feign.Request.create(feign.Request.HttpMethod.GET, "http://x", Map.of(), null, StandardCharsets.UTF_8, null);
    }
}
