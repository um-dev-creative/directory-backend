package com.prx.directory.client.mercury;

import com.prx.directory.client.backbone.BackboneManagedClient;
import com.prx.directory.client.backbone.to.ManagedClientTokenRequest;
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
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

/// Mercury's M2M token for Directory. Directory does not log in to Mercury with an alias and a password: it asks backbone-rest for the
/// token of its own managed client (`POST /api/v1/managed-clients/token`), sends it as `Authorization: Bearer` to Mercury's
/// `POST /api/v1/auth/token`, and Mercury answers its own token, tied to the Directory application, which goes in `session-token`.
/// Both tokens are kept until a minute before they expire, never persisted and never logged.
@Component
public class MercuryTokens {

    private static final Logger LOGGER = LoggerFactory.getLogger(MercuryTokens.class);
    static final Duration MARGIN = Duration.ofMinutes(1);
    static final Duration FALLBACK = Duration.ofMinutes(5);
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

    /// Builds the provider.
    ///
    /// @param backbone     the managed-client token endpoint of backbone-rest
    /// @param mercury      Mercury's token exchange
    /// @param clock        the clock the expiry is measured with
    /// @param clientId     Directory's managed client in Backbone (`MERCURY_M2M_CLIENT_ID`); empty = not configured
    /// @param clientSecret its secret (`MERCURY_M2M_CLIENT_SECRET`)
    /// @param scopes       the Mercury scopes asked for (`mercury:message:read` and `mercury:message:write` by default)
    @Autowired
    public MercuryTokens(BackboneManagedClient backbone, MercuryAuthClient mercury,
                         @Value("${prx.clients.mercury.m2m.client-id:}") String clientId,
                         @Value("${prx.clients.mercury.m2m.client-secret:}") String clientSecret,
                         @Value("${prx.clients.mercury.m2m.scopes:mercury:message:read,mercury:message:write}") List<String> scopes) {
        this(backbone, mercury, Clock.systemUTC(), clientId, clientSecret, scopes);
    }

    MercuryTokens(BackboneManagedClient backbone, MercuryAuthClient mercury, Clock clock, String clientId, String clientSecret, List<String> scopes) {
        this.backbone = backbone;
        this.mercury = mercury;
        this.clock = clock;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scopes = scopes;
    }

    /// Runs a Mercury call with the M2M token; when Mercury answers 401 (token revoked or expired early) both caches are dropped and the
    /// call is made once more with fresh tokens.
    ///
    /// @param call the call, given the Mercury token
    /// @param <T>  its result
    /// @return what the call returns
    public <T> T withToken(Function<String, T> call) {
        try {
            return call.apply(token());
        } catch (FeignException.Unauthorized e) {
            LOGGER.warn("Mercury refused the M2M token; asking for a new one");
            invalidate();
            return call.apply(token());
        }
    }

    /// Mercury's M2M token: the cached one, or a new one.
    ///
    /// @return the token for the `session-token` header
    /// @throws IllegalStateException when Directory's managed client is not configured
    public String token() {
        synchronized (lock) {
            var now = clock.instant();
            if (now.isBefore(mercuryToken.validUntil())) {
                return mercuryToken.token();
            }
            var bearer = "Bearer " + backboneToken(now);
            var issued = mercury.exchange(bearer).token();
            mercuryToken = new Cached(issued, expiryOf(issued).map(e -> e.minus(MARGIN)).orElse(now.plus(FALLBACK)));
            return issued;
        }
    }

    /// Forgets both tokens, so the next call asks for new ones.
    public void invalidate() {
        synchronized (lock) {
            backboneToken = Cached.NONE;
            mercuryToken = Cached.NONE;
        }
    }

    private String backboneToken(Instant now) {
        if (now.isBefore(backboneToken.validUntil())) {
            return backboneToken.token();
        }
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalStateException("Directory's Backbone managed client is not configured: set MERCURY_M2M_CLIENT_ID and MERCURY_M2M_CLIENT_SECRET");
        }
        var issued = backbone.token(new ManagedClientTokenRequest(UUID.fromString(clientId), clientSecret, scopes));
        var life = issued.expiresIn() > 0 ? Duration.ofSeconds(issued.expiresIn()).minus(MARGIN) : FALLBACK;
        backboneToken = new Cached(issued.accessToken(), now.plus(life.isNegative() ? Duration.ZERO : life));
        return issued.accessToken();
    }

    /// The `exp` of a JWT payload; empty when the token is not one we can read (the fallback life applies).
    private static java.util.Optional<Instant> expiryOf(String jwt) {
        try {
            var payload = new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
            var matcher = EXP.matcher(payload);
            return matcher.find() ? java.util.Optional.of(Instant.ofEpochSecond(Long.parseLong(matcher.group(1)))) : java.util.Optional.empty();
        } catch (RuntimeException e) {
            return java.util.Optional.empty();
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
