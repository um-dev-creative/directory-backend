package com.umdc.directory.client.mercury;

import com.umdc.security.to.AuthRequest;
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
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/// Mercury's token for Directory (MER-11). Directory logs in with its registered alias and password alone
/// (`POST /api/v1/auth/token`); the token carries no user and is only good for `/api/v1/verification-code` and `/api/v1/mail`.
/// It is kept until a minute before it expires, never persisted and never logged.
///
/// Failure rules: a refused login (401) is never retried, and nothing is sent to Mercury for a minute afterwards so wrong credentials
/// cannot trip Mercury's throttle (about 5 failures in 15 minutes); a 429 is honoured: nothing is sent until its `Retry-After` has passed.
@Component
public class MercuryTokens {

    private static final Logger LOGGER = LoggerFactory.getLogger(MercuryTokens.class);
    static final Duration MARGIN = Duration.ofMinutes(1);
    static final Duration FALLBACK = Duration.ofMinutes(5);
    /// Quiet time after a refused login.
    static final Duration LOGIN_BACKOFF = Duration.ofMinutes(1);
    /// Wait used when a 429 carries no usable `Retry-After`.
    static final long DEFAULT_RETRY_AFTER_SECONDS = 60;
    private static final int TOO_MANY_REQUESTS = 429;
    private static final Pattern EXP = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");

    private final MercuryAuthClient mercury;
    private final Clock clock;
    private final String alias;
    private final String password;

    private final Object lock = new Object();
    private Cached mercuryToken = Cached.NONE;
    private Instant throttledUntil = Instant.MIN;
    private boolean throttledByMercury;

    /// Builds the provider.
    ///
    /// @param mercury  Mercury's token endpoint
    /// @param alias    Directory's login alias at Mercury (`DIRECTORY_BACKEND_LOGIN_ALIAS`); empty = not configured
    /// @param password its password (`DIRECTORY_BACKEND_LOGIN_PASSWORD`)
    @Autowired
    public MercuryTokens(MercuryAuthClient mercury,
                         @Value("${umdc.clients.mercury.login.alias:${DIRECTORY_BACKEND_LOGIN_ALIAS:}}") String alias,
                         @Value("${umdc.clients.mercury.login.password:${DIRECTORY_BACKEND_LOGIN_PASSWORD:}}") String password) {
        this(mercury, Clock.systemUTC(), alias, password);
    }

    MercuryTokens(MercuryAuthClient mercury, Clock clock, String alias, String password) {
        this.mercury = mercury;
        this.clock = clock;
        this.alias = alias;
        this.password = password;
    }

    /// Runs a Mercury call with the token. When Mercury answers 401 to the call (token revoked or expired early) the token is dropped and
    /// the call is made once more with a new one; a 401 of the login itself is not retried. A 429 starts the wait of its `Retry-After`.
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
            LOGGER.warn("Mercury refused the token; logging in again once");
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

    /// Mercury's token: the cached one, or a new one.
    ///
    /// @return the token for the `session-token` header
    /// @throws MercuryThrottledException   while a `Retry-After` of Mercury has not passed
    /// @throws MercuryUnavailableException when Directory's login is not configured, was refused, or Mercury failed
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
                throw new MercuryUnavailableException("Mercury refused Directory's login recently; not trying again yet.");
            }
            if (alias.isBlank() || password.isBlank()) {
                throw new MercuryUnavailableException("Directory's Mercury login is not configured: set DIRECTORY_BACKEND_LOGIN_ALIAS and DIRECTORY_BACKEND_LOGIN_PASSWORD");
            }
            return login(now);
        }
    }

    private String login(Instant now) {
        try {
            var issued = mercury.token(new AuthRequest(alias, password)).token();
            if (issued == null || issued.isBlank()) {
                throw new MercuryUnavailableException("Mercury answered without a token.");
            }
            mercuryToken = new Cached(issued, expiryOf(issued).map(e -> e.minus(MARGIN)).orElse(now.plus(FALLBACK)));
            return issued;
        } catch (FeignException.Unauthorized e) {
            LOGGER.error("Mercury refused Directory's login (401); check the alias and password. Not retrying for {}", LOGIN_BACKOFF);
            throttledUntil = now.plus(LOGIN_BACKOFF);
            throttledByMercury = false;
            throw new MercuryUnavailableException("Mercury refused Directory's login.", e);
        } catch (FeignException e) {
            if (e.status() == TOO_MANY_REQUESTS) {
                throw throttled(e);
            }
            throw new MercuryUnavailableException("Mercury login failed with status " + e.status() + ".", e);
        } catch (MercuryUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new MercuryUnavailableException("Mercury login failed.", e);
        }
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

    /// Forgets the token, so the next call logs in again. It does not lift a wait that Mercury asked for.
    public void invalidate() {
        synchronized (lock) {
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
