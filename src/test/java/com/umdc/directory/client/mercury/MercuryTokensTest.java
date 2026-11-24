package com.umdc.directory.client.mercury;

import com.umdc.directory.api.v1.to.PrxTokenString;
import com.umdc.security.to.AuthRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercuryTokensTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private MercuryAuthClient mercury;

    private MutableClock clock;
    private MercuryTokens tokens;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(NOW);
        tokens = new MercuryTokens(mercury, clock, "directory", "pw");
    }

    private static feign.FeignException feign(int status, Map<String, Collection<String>> headers) {
        var request = feign.Request.create(feign.Request.HttpMethod.GET, "/test", Map.of(), null, StandardCharsets.UTF_8, null);
        var response = feign.Response.builder().status(status).reason("test").request(request).headers(headers).build();
        return feign.FeignException.errorStatus("test", response);
    }

    private static feign.FeignException feign(int status) {
        return feign(status, Map.of());
    }

    private static String jwtExpiringAt(Instant exp) {
        var payload = Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"sub\":\"x\",\"exp\":" + exp.getEpochSecond() + "}").getBytes(StandardCharsets.UTF_8));
        return "h." + payload + ".s";
    }

    @Test
    void logsInWithTheAliasAndPasswordAlone() {
        when(mercury.token(new AuthRequest("directory", "pw"))).thenReturn(new PrxTokenString("mercury-token"));

        assertEquals("mercury-token", tokens.token());
    }

    @Test
    void keepsTheTokenUntilAMinuteBeforeItExpires() {
        var token = jwtExpiringAt(NOW.plus(Duration.ofMinutes(10)));
        when(mercury.token(any())).thenReturn(new PrxTokenString(token));

        assertEquals(token, tokens.token());
        clock.set(NOW.plus(Duration.ofMinutes(8)));
        assertEquals(token, tokens.token());
        verify(mercury, times(1)).token(any());

        clock.set(NOW.plus(Duration.ofMinutes(9)).plusSeconds(1));
        tokens.token();
        verify(mercury, times(2)).token(any());
    }

    @Test
    void anUnreadableTokenIsKeptForTheFallbackTime() {
        when(mercury.token(any())).thenReturn(new PrxTokenString("not-a-jwt"), new PrxTokenString("second"));

        assertEquals("not-a-jwt", tokens.token());
        clock.set(NOW.plus(MercuryTokens.FALLBACK).minusSeconds(1));
        assertEquals("not-a-jwt", tokens.token());
        clock.set(NOW.plus(MercuryTokens.FALLBACK));
        assertEquals("second", tokens.token());
    }

    @Test
    void refusesToCallMercuryWhenTheLoginIsNotConfigured() {
        var unconfigured = new MercuryTokens(mercury, clock, "", "");

        var error = assertThrows(MercuryUnavailableException.class, unconfigured::token);

        assertTrue(error.getMessage().contains("DIRECTORY_BACKEND_LOGIN_ALIAS"));
        verify(mercury, never()).token(any());
    }

    @Test
    void aBlankPasswordIsNotConfiguredEither() {
        var unconfigured = new MercuryTokens(mercury, clock, "directory", " ");

        assertThrows(MercuryUnavailableException.class, unconfigured::token);
    }

    @Test
    void aRefusedLoginIsNotRetriedAndMercuryIsLeftAloneForAMinute() {
        when(mercury.token(any())).thenThrow(feign(401));

        var error = assertThrows(MercuryUnavailableException.class, tokens::token);
        assertFalse(error.getMessage().contains("pw"));
        assertThrows(MercuryUnavailableException.class, tokens::token);
        verify(mercury, times(1)).token(any());

        clock.set(NOW.plus(MercuryTokens.LOGIN_BACKOFF));
        assertThrows(MercuryUnavailableException.class, tokens::token);
        verify(mercury, times(2)).token(any());
    }

    @Test
    void aThrottledLoginHonoursRetryAfterAndSendsNothingMeanwhile() {
        when(mercury.token(any())).thenThrow(feign(429, Map.of("Retry-After", List.of("120"))));

        var first = assertThrows(MercuryThrottledException.class, tokens::token);
        assertEquals(120, first.getRetryAfterSeconds());

        clock.set(NOW.plusSeconds(100));
        var second = assertThrows(MercuryThrottledException.class, tokens::token);
        assertEquals(20, second.getRetryAfterSeconds());
        verify(mercury, times(1)).token(any());

        clock.set(NOW.plusSeconds(120));
        org.mockito.Mockito.doReturn(new PrxTokenString("m")).when(mercury).token(any());
        assertEquals("m", tokens.token());
    }

    @Test
    void aThrottleWithoutAUsableRetryAfterWaitsTheDefault() {
        when(mercury.token(any())).thenThrow(feign(429, Map.of("retry-after", List.of("Wed, 21 Oct 2026 07:28:00 GMT"))));

        var error = assertThrows(MercuryThrottledException.class, tokens::token);

        assertEquals(MercuryTokens.DEFAULT_RETRY_AFTER_SECONDS, error.getRetryAfterSeconds());
    }

    @Test
    void aZeroRetryAfterWaitsTheDefault() {
        when(mercury.token(any())).thenThrow(feign(429, Map.of("Retry-After", List.of("0"))));

        assertEquals(MercuryTokens.DEFAULT_RETRY_AFTER_SECONDS, assertThrows(MercuryThrottledException.class, tokens::token).getRetryAfterSeconds());
    }

    @Test
    void otherLoginFailuresAreUnavailable() {
        when(mercury.token(any())).thenThrow(feign(500));

        assertTrue(assertThrows(MercuryUnavailableException.class, tokens::token).getMessage().contains("500"));
    }

    @Test
    void aTransportFailureOfTheLoginIsUnavailable() {
        when(mercury.token(any())).thenThrow(new IllegalStateException("connection reset"));

        assertThrows(MercuryUnavailableException.class, tokens::token);
    }

    @Test
    void anEmptyAnswerIsUnavailable() {
        when(mercury.token(any())).thenReturn(new PrxTokenString(" "));

        assertThrows(MercuryUnavailableException.class, tokens::token);
    }

    @Test
    void aCallThatGets401LogsInAgainOnceAndRepeats() {
        when(mercury.token(any())).thenReturn(new PrxTokenString("m1"), new PrxTokenString("m2"));
        var calls = new AtomicInteger();

        var result = tokens.withToken(token -> {
            if (calls.getAndIncrement() == 0) {
                throw feign(401);
            }
            return "done with " + token;
        });

        assertEquals("done with m2", result);
        assertEquals(2, calls.get());
    }

    @Test
    void aSecond401IsNotRetriedAgain() {
        when(mercury.token(any())).thenReturn(new PrxTokenString("m"));
        var calls = new AtomicInteger();

        assertThrows(feign.FeignException.Unauthorized.class, () -> tokens.withToken(token -> {
            calls.incrementAndGet();
            throw feign(401);
        }));
        assertEquals(2, calls.get());
    }

    @Test
    void aLoginRefusedWhileRetryingIsNotLooped() {
        when(mercury.token(any())).thenReturn(new PrxTokenString("m1")).thenThrow(feign(401));

        assertThrows(MercuryUnavailableException.class, () -> tokens.withToken(token -> {
            throw feign(401);
        }));
        verify(mercury, times(2)).token(any());
    }

    @Test
    void aCallThatGets429IsNotRetriedAndBlocksTheNextOnes() {
        when(mercury.token(any())).thenReturn(new PrxTokenString("m"));
        var calls = new AtomicInteger();

        var error = assertThrows(MercuryThrottledException.class, () -> tokens.withToken(token -> {
            calls.incrementAndGet();
            throw feign(429, Map.of("Retry-After", List.of("30")));
        }));

        assertEquals(30, error.getRetryAfterSeconds());
        assertEquals(1, calls.get());
        assertThrows(MercuryThrottledException.class, () -> tokens.withToken(token -> "never"));
    }

    @Test
    void aRetriedCallThatGets429IsThrottled() {
        when(mercury.token(any())).thenReturn(new PrxTokenString("m"));
        var calls = new AtomicInteger();

        assertThrows(MercuryThrottledException.class, () -> tokens.withToken(token -> {
            if (calls.getAndIncrement() == 0) {
                throw feign(401);
            }
            throw feign(429);
        }));
        assertEquals(2, calls.get());
    }

    @Test
    void otherFailuresPassThroughWithoutANewLogin() {
        when(mercury.token(any())).thenReturn(new PrxTokenString("m"));

        assertThrows(feign.FeignException.class, () -> tokens.withToken(token -> {
            throw feign(500);
        }));
        verify(mercury, times(1)).token(any());
    }

    @Test
    void invalidateForcesANewLogin() {
        when(mercury.token(any())).thenReturn(new PrxTokenString("a"), new PrxTokenString("b"));

        assertEquals("a", tokens.token());
        tokens.invalidate();
        assertEquals("b", tokens.token());
    }

    /// A clock the test moves by hand.
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void set(Instant instant) {
            this.now = instant;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
