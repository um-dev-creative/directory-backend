package com.prx.directory.client.mercury;

import com.prx.directory.client.backbone.BackboneManagedClient;
import com.prx.directory.client.backbone.to.ManagedClientToken;
import com.prx.directory.client.backbone.to.ManagedClientTokenRequest;
import com.prx.directory.api.v1.to.PrxTokenString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercuryTokensTest {

    private static final UUID CLIENT = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final List<String> SCOPES = List.of("mercury:message:read", "mercury:message:write");

    @Mock
    private BackboneManagedClient backbone;
    @Mock
    private MercuryAuthClient mercury;

    private MutableClock clock;
    private MercuryTokens tokens;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(NOW);
        tokens = new MercuryTokens(backbone, mercury, clock, CLIENT.toString(), "secret", SCOPES);
    }

    private static feign.FeignException feign(int status) {
        var request = feign.Request.create(feign.Request.HttpMethod.GET, "/test", java.util.Map.of(), null, StandardCharsets.UTF_8, null);
        var response = feign.Response.builder().status(status).reason("test").request(request).build();
        return feign.FeignException.errorStatus("test", response);
    }

    private static String jwtExpiringAt(Instant exp) {
        var payload = Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"sub\":\"x\",\"exp\":" + exp.getEpochSecond() + "}").getBytes(StandardCharsets.UTF_8));
        return "h." + payload + ".s";
    }

    @Test
    void tradesTheBackboneTokenForMercurysAndSendsItAsBearer() {
        when(backbone.token(any())).thenReturn(new ManagedClientToken("backbone-token", 300));
        when(mercury.exchange("Bearer backbone-token")).thenReturn(new PrxTokenString("mercury-token"));

        assertEquals("mercury-token", tokens.token());

        var request = ArgumentCaptor.forClass(ManagedClientTokenRequest.class);
        verify(backbone).token(request.capture());
        assertEquals(new ManagedClientTokenRequest(CLIENT, "secret", SCOPES), request.getValue());
    }

    @Test
    void keepsMercurysTokenUntilAMinuteBeforeItExpires() {
        var mercuryToken = jwtExpiringAt(NOW.plus(Duration.ofMinutes(10)));
        when(backbone.token(any())).thenReturn(new ManagedClientToken("b", 3600));
        when(mercury.exchange(anyString())).thenReturn(new PrxTokenString(mercuryToken));

        assertEquals(mercuryToken, tokens.token());
        clock.set(NOW.plus(Duration.ofMinutes(8)));
        assertEquals(mercuryToken, tokens.token());
        verify(mercury, times(1)).exchange(anyString());

        clock.set(NOW.plus(Duration.ofMinutes(9)).plusSeconds(1));
        tokens.token();
        verify(mercury, times(2)).exchange(anyString());
        verify(backbone, times(1)).token(any());   // the Backbone token (1 h) is still good
    }

    @Test
    void asksBackboneAgainWhenItsTokenExpired() {
        when(backbone.token(any())).thenReturn(new ManagedClientToken("b", 120));
        when(mercury.exchange(anyString())).thenReturn(new PrxTokenString("opaque"));   // no readable exp: kept 5 minutes

        tokens.token();
        clock.set(NOW.plus(Duration.ofMinutes(6)));
        tokens.token();

        verify(backbone, times(2)).token(any());
        verify(mercury, times(2)).exchange(anyString());
    }

    @Test
    void anUnreadableMercuryTokenIsKeptForTheFallbackTime() {
        when(backbone.token(any())).thenReturn(new ManagedClientToken("b", 0));
        when(mercury.exchange(anyString())).thenReturn(new PrxTokenString("not-a-jwt"), new PrxTokenString("second"));

        assertEquals("not-a-jwt", tokens.token());
        clock.set(NOW.plus(MercuryTokens.FALLBACK).minusSeconds(1));
        assertEquals("not-a-jwt", tokens.token());
        clock.set(NOW.plus(MercuryTokens.FALLBACK));
        assertEquals("second", tokens.token());
    }

    @Test
    void aShortBackboneLifeNeverGoesNegative() {
        when(backbone.token(any())).thenReturn(new ManagedClientToken("b", 10));
        when(mercury.exchange(anyString())).thenReturn(new PrxTokenString("m"));

        assertEquals("m", tokens.token());
    }

    @Test
    void refusesToCallAnythingWhenTheManagedClientIsNotConfigured() {
        var unconfigured = new MercuryTokens(backbone, mercury, clock, "", "", SCOPES);

        var error = assertThrows(IllegalStateException.class, unconfigured::token);

        assertTrue(error.getMessage().contains("MERCURY_M2M_CLIENT_ID"));
        verify(backbone, never()).token(any());
        verify(mercury, never()).exchange(anyString());
    }

    @Test
    void aMercury401DropsBothTokensAndRetriesOnce() {
        when(backbone.token(any())).thenReturn(new ManagedClientToken("b1", 3600), new ManagedClientToken("b2", 3600));
        when(mercury.exchange("Bearer b1")).thenReturn(new PrxTokenString("m1"));
        when(mercury.exchange("Bearer b2")).thenReturn(new PrxTokenString("m2"));
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
        when(backbone.token(any())).thenReturn(new ManagedClientToken("b", 3600));
        when(mercury.exchange(anyString())).thenReturn(new PrxTokenString("m"));

        assertThrows(feign.FeignException.Unauthorized.class, () -> tokens.withToken(token -> {
            throw feign(401);
        }));
    }

    @Test
    void otherFailuresPassThroughWithoutNewTokens() {
        when(backbone.token(any())).thenReturn(new ManagedClientToken("b", 3600));
        when(mercury.exchange(anyString())).thenReturn(new PrxTokenString("m"));

        assertThrows(feign.FeignException.class, () -> tokens.withToken(token -> {
            throw feign(500);
        }));
        verify(backbone, times(1)).token(any());
    }

    @Test
    void theRecordsNeverPrintSecrets() {
        assertTrue(!new ManagedClientTokenRequest(CLIENT, "secret", SCOPES).toString().contains("secret"));
        assertTrue(!new ManagedClientToken("backbone-token", 1).toString().contains("backbone-token"));
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
