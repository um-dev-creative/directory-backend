package com.umdc.directory.api.v1.service;

import com.umdc.directory.client.mercury.MercuryClient;
import com.umdc.directory.client.mercury.MercuryThrottledException;
import com.umdc.directory.client.mercury.MercuryTokens;
import com.umdc.directory.client.mercury.MercuryUnavailableException;
import com.umdc.directory.client.mercury.to.VerificationCodeRequest;
import com.umdc.directory.kafka.producer.EmailMessageProducerService;
import com.umdc.directory.kafka.to.EmailMessageTO;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MercuryVerificationServiceTest {

    private final MercuryClient client = mock(MercuryClient.class);
    private final MercuryTokens tokens = mock(MercuryTokens.class);
    private final EmailMessageProducerService producer = mock(EmailMessageProducerService.class);
    private final UUID user = UUID.randomUUID();
    private final UUID application = UUID.randomUUID();
    private MercuryVerificationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new MercuryVerificationService(client, tokens, producer);
        ReflectionTestUtils.setField(service, "templateId", UUID.randomUUID().toString());
        ReflectionTestUtils.setField(service, "applicationId", application.toString());
        when(tokens.withToken(any())).thenAnswer(inv -> ((Function<String, Object>) inv.getArgument(0)).apply("mercury-token"));
    }

    private static FeignException feign(int status) {
        var request = feign.Request.create(feign.Request.HttpMethod.GET, "/t", Map.of(), null, StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("t", feign.Response.builder().status(status).reason("r").request(request).build());
    }

    @Test
    void sendCodeQueuesTheEmailWithTheCodeAsParameter() {
        service.sendCode(user, "owner@x.com", "Owner", "123456789");

        var message = ArgumentCaptor.forClass(EmailMessageTO.class);
        verify(producer).sendMessage(message.capture());
        assertEquals(user, message.getValue().userId());
        assertEquals("owner@x.com", message.getValue().to().getFirst().email());
        assertEquals("123456789", message.getValue().params().get("vc"));
        assertFalse(message.getValue().body().contains("123456789"));
    }

    @Test
    void sendCodeWithoutTemplateOrNameStillQueues() {
        ReflectionTestUtils.setField(service, "templateId", " ");

        service.sendCode(user, "owner@x.com", null, "123456789");

        var message = ArgumentCaptor.forClass(EmailMessageTO.class);
        verify(producer).sendMessage(message.capture());
        assertNull(message.getValue().templateDefinedId());
        assertEquals("", message.getValue().params().get("user_name"));
    }

    @Test
    void sendCodeFailureIsUnavailableAndDoesNotLeakTheCode() {
        doThrow(new IllegalStateException("kafka down 123456789")).when(producer).sendMessage(any());

        var error = assertThrows(MercuryUnavailableException.class, () -> service.sendCode(user, "o@x.com", "O", "123456789"));

        assertFalse(error.getMessage().contains("123456789"));
    }

    @Test
    void verifyCodeIsTrueWhenMercuryAnswers2xx() {
        when(client.confirmCode(eq("mercury-token"), any(VerificationCodeRequest.class))).thenReturn(ResponseEntity.ok().build());

        assertTrue(service.verifyCode(user, "123456789"));
        var request = ArgumentCaptor.forClass(VerificationCodeRequest.class);
        verify(client).confirmCode(eq("mercury-token"), request.capture());
        assertEquals(application, request.getValue().applicationId());
        assertEquals(user, request.getValue().userId());
    }

    @Test
    void verifyCodeIsFalseWhenMercuryRejectsIt() {
        when(client.confirmCode(anyString(), any())).thenThrow(feign(400));
        assertFalse(service.verifyCode(user, "123456789"));
    }

    @Test
    void verifyCodeIsFalseOnAnEmptyAnswerOrANon2xx() {
        when(client.confirmCode(anyString(), any())).thenReturn(null, ResponseEntity.accepted().build(), ResponseEntity.status(204).build());
        assertFalse(service.verifyCode(user, "123456789"));
        assertTrue(service.verifyCode(user, "123456789"));
        assertTrue(service.verifyCode(user, "123456789"));
        when(client.confirmCode(anyString(), any())).thenReturn(ResponseEntity.status(302).build());
        assertFalse(service.verifyCode(user, "123456789"));
    }

    @Test
    void verifyCodeServerFailureIsUnavailable() {
        when(client.confirmCode(anyString(), any())).thenThrow(feign(502));
        assertThrows(MercuryUnavailableException.class, () -> service.verifyCode(user, "123456789"));
    }

    @Test
    void verifyCodeLetsThrottlingAndUnavailabilityThrough() {
        doThrow(new MercuryThrottledException(9)).when(tokens).withToken(any());
        assertEquals(9, assertThrows(MercuryThrottledException.class, () -> service.verifyCode(user, "1")).getRetryAfterSeconds());
        doThrow(new MercuryUnavailableException("x")).when(tokens).withToken(any());
        assertThrows(MercuryUnavailableException.class, () -> service.verifyCode(user, "1"));
    }

    @Test
    void verifyCodeAnyOtherFailureIsUnavailable() {
        doThrow(new IllegalStateException("x")).when(tokens).withToken(any());
        assertThrows(MercuryUnavailableException.class, () -> service.verifyCode(user, "1"));
    }

    @Test
    void verifyCodeWithoutAnApplicationIdIsUnavailable() {
        ReflectionTestUtils.setField(service, "applicationId", "");
        assertThrows(MercuryUnavailableException.class, () -> service.verifyCode(user, "1"));
    }

    @Test
    void isVerifiedReadsTheStatus() {
        when(client.isVerificationCodeDone("mercury-token", user)).thenReturn(true, false, null);

        assertTrue(service.isVerified(user));
        assertFalse(service.isVerified(user));
        assertFalse(service.isVerified(user));
    }

    @Test
    void isVerifiedIsFalseWhenMercuryHasNoCode() {
        when(client.isVerificationCodeDone(anyString(), any())).thenThrow(feign(404));
        assertFalse(service.isVerified(user));
    }

    @Test
    void isVerifiedOtherStatusesAreUnavailable() {
        when(client.isVerificationCodeDone(anyString(), any())).thenThrow(feign(500));
        assertThrows(MercuryUnavailableException.class, () -> service.isVerified(user));
    }

    @Test
    void isVerifiedLetsThrottlingAndUnavailabilityThrough() {
        doThrow(new MercuryThrottledException(3)).when(tokens).withToken(any());
        assertThrows(MercuryThrottledException.class, () -> service.isVerified(user));
        doThrow(new MercuryUnavailableException("x")).when(tokens).withToken(any());
        assertThrows(MercuryUnavailableException.class, () -> service.isVerified(user));
        doThrow(new IllegalStateException("x")).when(tokens).withToken(any());
        assertThrows(MercuryUnavailableException.class, () -> service.isVerified(user));
    }
}
