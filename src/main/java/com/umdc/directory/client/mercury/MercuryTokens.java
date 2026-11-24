package com.umdc.directory.client.mercury;

import com.umdc.directory.client.backbone.BackboneManagedClient;
import com.umdc.directory.client.backbone.to.ManagedClientTokenRequest;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

/// Mercury's M2M token for Directory. Directory does NOT log in to Mercury with an alias and a password: it asks backbone-rest for the
/// Backbone M2M token of its own managed client (`POST /api/v1/managed-clients/token`, client credentials), sends it as
/// `Authorization: Bearer` to Mercury's `POST /api/v1/auth/token`, and Mercury answers its own M2M token, tied to the application Backbone
/// links the client to. That token goes in `session-token` and carries no user: it is only good for `/api/v1/verification-code` and
/// `/api/v1/mail`. Both tokens are kept until a minute before they expire, never persisted and never logged.
///
/// Failure rules: a refused token (401/403) of Backbone or Mercury is never retried in a loop, and nothing is sent for a minute afterwards;
/// a 429 is honoured: nothing is sent until its `Retry-After` has passed; a call that Mercury answers 401 gets one fresh pair of tokens
/// and one repeat, no more.
@Component
public class MercuryTokens {

    private static final Logger LOGGER = LoggerFactory.getLogger(MercuryTokens.class);
    static final Duration MARGIN = Duration.ofMinutes(1);
    static final Duration FALLBACK = Duration.ofMinutes(5);
    /// Quiet time after a refused token.
    static final Duration LOGIN_BACKOFF = Duration.ofMinutes(1);
    /// Wait used when a 429 carries no usable `Retry-After`.
    static final long DEFAULT_RETRY_AFTER_SECONDS = 60;
    private static final int TOO_MANY_REQUESTS = 429;
    private static final Pattern EXP = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");

    private final BackboneManagedClient backbone;
    private final MercuryAuthClient mercury;
    private final Clock clock;
    private final String clientId;
    private final String clientSecret;
    private final List<String> scopes;

    private final Object lock = new Object();
    private Cached backboneToken = Cached.NONE;
    private Cached mercuryToken = Cached.NONE;
    private Instant throttledUntil = Instant.MIN;
    private boolean throttledByMercury;

    /// Builds the provider.
    ///
    /// @param backbone     the managed-client token endpoint of backbone-rest
    /// @param mercury      Mercury's token exchange
    /// @param clientId     Directory's managed client in Backbone (`MERCURY_M2M_CLIENT_ID`); empty = not configured
    /// @param clientSecret its secret (`MERCURY_M2M_CLIENT_SECRET`)
    /// @param scopes       the Mercury scopes asked for (`mercury:message:read` and `mercury:message:write` by default)
    @Autowired
    public MercuryTokens(BackboneManagedClient backbone, MercuryAuthClient mercury,
                         @Value("${umdc.clients.mercury.m2m.client-id:}") String clientId,
                         @Value("${umdc.clients.mercury.m2m.client-secret:}") String clientSecret,
                         @Value("${umdc.clients.mercury.m2m.scopes:mercury:message:read,mercury:message:write}") List<String> scopes) {
        this(backbone, mercury, Clock.systemUTC(), clientId, clientSecret, scopes);
    }

    MercuryTokens(BackboneManagedClient backbone, MercuryAuthClient mercury, Clock clock, String clientId, String clientSecret,
                  List<String> scopes) {
        this.backbone = backbone;
        this.mercury = mercury;
        this.clock = clock;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scopes = scopes;
    }

    /// Runs a Mercury call with the token. When Mercury answers 401 to the call (token revoked or expired early) the token is dropped and
    /// the call is made once more with new tokens; a 401 of the token requests themselves is not retried. A 429 starts the wait of its `Retry-After`.
    ///
    /// @param call the call, given the Mercury token
    /// @param <T>  its result
    /// @return what the call returns
    /// @throws MercuryThrottledException   when Mercury is throttling this client
    /// @throws MercuryUnavailableException when Mercury cannot be logged in to
    public <T> T withToken(Function<String, T> call) {
        var token = token();
        try {
            return call.apply(token);
        } catch (FeignException.Unauthorized e) {
            LOGGER.warn("Mercury refused the M2M token; asking for a new one once");
            invalidate();
        } catch (FeignException e) {
            if (e.status() == TOO_MANY_REQUESTS) {
                throw throttled(e);
            }
            throw e;
        }
        try {
            return call.apply(token());
        } catch (FeignException e) {
            if (e.status() == TOO_MANY_REQUESTS) {
                throw throttled(e);
            }
            throw e;
        }
    }

    /// Mercury's M2M token: the cached one, or a new one.
    ///
    /// @return the token for the `session-token` header
    /// @throws MercuryThrottledException   while a `Retry-After` of Mercury or Backbone has not passed
    /// @throws MercuryUnavailableException when Directory's managed client is not configured, was refused, or Backbone/Mercury failed
    public String token() {
        synchronized (lock) {
            var now = clock.instant();
            var waiting = now.isBefore(throttledUntil);
            if (waiting && throttledByMercury) {
                throw new MercuryThrottledException(Math.max(1, Duration.between(now, throttledUntil).toSeconds()));
            }
            if (now.isBefore(mercuryToken.validUntil())) {
                return mercuryToken.token();
            }
            if (waiting) {
                throw new MercuryUnavailableException("A token of Directory's managed client was refused recently; not trying again yet.");
            }
            return exchange(now);
        }
    }

    private String exchange(Instant now) {
        try {
            var bearer = "Bearer " + backboneToken(now);
            var issued = mercury.exchange(bearer).token();
            if (issued == null || issued.isBlank()) {
                throw new MercuryUnavailableException("Mercury answered without a token.");
            }
            mercuryToken = new Cached(issued, expiryOf(issued).map(e -> e.minus(MARGIN)).orElse(now.plus(FALLBACK)));
            return issued;
        } catch (FeignException.Unauthorized | FeignException.Forbidden | FeignException.BadRequest e) {
            LOGGER.error("A token request of Directory's managed client was refused ({}); check MERCURY_M2M_CLIENT_ID, MERCURY_M2M_CLIENT_SECRET and its scopes. Not retrying for {}",
                    e.status(), LOGIN_BACKOFF);
            backboneToken = Cached.NONE;
            throttledUntil = now.plus(LOGIN_BACKOFF);
            throttledByMercury = false;
            throw new MercuryUnavailableException("Mercury or Backbone refused Directory's managed client.", e);
        } catch (FeignException e) {
            if (e.status() == TOO_MANY_REQUESTS) {
                throw throttled(e);
            }
            throw new MercuryUnavailableException("Mercury M2M token exchange failed with status " + e.status() + ".", e);
        } catch (MercuryUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new MercuryUnavailableException("Mercury M2M token exchange failed.", e);
        }
    }

    private String backboneToken(Instant now) {
        if (now.isBefore(backboneToken.validUntil())) {
            return backboneToken.token();
        }
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new MercuryUnavailableException("Directory's Backbone managed client is not configured: set MERCURY_M2M_CLIENT_ID and MERCURY_M2M_CLIENT_SECRET");
        }
        UUID id;
        try {
            id = UUID.fromString(clientId.trim());
        } catch (IllegalArgumentException e) {
            throw new MercuryUnavailableException("MERCURY_M2M_CLIENT_ID is not a UUID.", e);
        }
        var issued = backbone.token(new ManagedClientTokenRequest(id, clientSecret, scopes));
        if (issued == null || issued.accessToken() == null || issued.accessToken().isBlank()) {
            throw new MercuryUnavailableException("Backbone answered without a token.");
        }
        var life = issued.expiresIn() > 0 ? Duration.ofSeconds(issued.expiresIn()).minus(MARGIN) : FALLBACK;
        backboneToken = new Cached(issued.accessToken(), now.plus(life.isNegative() ? Duration.ZERO : life));
        return issued.accessToken();
    }

    /// Records the wait Mercury asked for and builds the exception for it.
    private MercuryThrottledException throttled(FeignException e) {
        var seconds = retryAfterSeconds(e);
        synchronized (lock) {
            throttledUntil = clock.instant().plusSeconds(seconds);
            throttledByMercury = true;
        }
        LOGGER.warn("Mercury answered 429; nothing is sent for {} seconds", seconds);
        return new MercuryThrottledException(seconds, e);
    }

    private static long retryAfterSeconds(FeignException e) {
        var values = e.responseHeaders().entrySet().stream()
                .filter(h -> "retry-after".equalsIgnoreCase(h.getKey()))
                .flatMap(h -> h.getValue().stream()).toList();
        for (var value : values) {
            try {
                var seconds = Long.parseLong(value.trim());
                if (seconds > 0) {
                    return seconds;
                }
            } catch (NumberFormatException ignored) {
                // an HTTP-date or garbage: use the default
            }
        }
        return DEFAULT_RETRY_AFTER_SECONDS;
    }

    /// Forgets both tokens, so the next call asks for new ones. It does not lift a wait that Mercury asked for.
    public void invalidate() {
        synchronized (lock) {
            backboneToken = Cached.NONE;
            mercuryToken = Cached.NONE;
        }
    }

    /// The `exp` of a JWT payload; empty when the token is not one we can read (the fallback life applies).
    private static Optional<Instant> expiryOf(String jwt) {
        try {
            var payload = new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
            var matcher = EXP.matcher(payload);
            return matcher.find() ? Optional.of(Instant.ofEpochSecond(Long.parseLong(matcher.group(1)))) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private record Cached(String token, Instant validUntil) {
        static final Cached NONE = new Cached("", Instant.MIN);

        @Override
        public String toString() {
            return "Cached[validUntil=" + validUntil + "]";
        }
    }
}
