# Authentication towards mercury (technical guide)

Directory authenticates to mercury with the Backbone M2M exchange and nothing else. Source of truth: mercury's own `application.yml`
("Backend applications do NOT log in to Mercury with a password: they exchange the Backbone M2M token of their managed client for Mercury's own
M2M token (`POST /api/v1/auth/token`, `Authorization: Bearer <Backbone token>`)", mercury commit 975e5c6). An earlier change (MER-11, PR #147)
used an alias/password login instead; mercury does not accept it, so it was removed and the exchange restored. The mercury token is cached and
used only for `/api/v1/verification-code` and `/api/v1/mail`.

```mermaid
sequenceDiagram
    participant D as Directory (MercuryTokens)
    participant B as backbone-rest
    participant M as mercury
    D->>B: POST /api/v1/managed-clients/token {clientId, clientSecret, scopes}
    B-->>D: Backbone M2M token (cached until 1 min before expiry)
    D->>M: POST /api/v1/auth/token, Authorization: Bearer Backbone token
    alt 200
        M-->>D: mercury token (no user)
        D->>M: POST /api/v1/verification-code, session-token: mercury token
        M-->>D: 2xx / 4xx
    else 401 or 403 on a token request
        Note over D: no retry; no token request for 1 minute (503 to the caller)
    else 401 on the business call
        Note over D: drop both tokens, ONE fresh pair and one repeat
    else 429
        M-->>D: 429 Retry-After: n
        Note over D: nothing sent for n seconds; 429 + Retry-After to the caller
    end
```

| Piece | Role |
| --- | --- |
| `client/backbone/BackboneManagedClient` | Feign client of backbone-rest's `POST /api/v1/managed-clients/token` (client credentials in the body). |
| `client/mercury/MercuryAuthClient` | Feign client of mercury's `POST /api/v1/auth/token`; `exchange(Bearer)` sends the Backbone token in `Authorization`. |
| `client/interceptor/MercuryAuthFeignConfig` | Configuration of both clients above: only timeouts (connect 3 s, read 5 s), no OAuth Bearer interceptor. |
| `client/mercury/MercuryTokens` | Runs the exchange and caches both tokens until one minute before `exp`/`expires_in` (5 minutes when unknown), thread-safe. `withToken(call)` runs a call; a 401 of the call drops both tokens and repeats it **once**; a refused token request is never retried; a 429 records the `Retry-After` and every later call fails fast with `MercuryThrottledException` until it passes. Never logs a secret or a token. |
| `client/mercury/MercuryClient` | Calls that carry the mercury token in `session-token` (`confirmCode`, `isVerificationCodeDone`). `MercuryFeignConfigurer` adds the OAuth Bearer and timeouts (3 s / 5 s). |
| `client/mercury/MercuryErrors` | `MercuryThrottledException` becomes 429 + `Retry-After`; `MercuryUnavailableException` (not configured, refused, 5xx, timeout) becomes 503. Both carry `message-error` and never a credential. |
| `api/v1/service/MercuryVerificationService` | `sendCode` (Kafka e-mail that mercury delivers), `verifyCode` (`POST /verification-code`), `isVerified` (`latest-status`). Never logs a code or a token. |

Configuration (names only; values come from Vault or the environment):

| Property | Environment variable | Default |
| --- | --- | --- |
| `umdc.clients.mercury.m2m.client-id` | `MERCURY_M2M_CLIENT_ID` | empty = not configured (the context starts; calls answer 503) |
| `umdc.clients.mercury.m2m.client-secret` | `MERCURY_M2M_CLIENT_SECRET` | empty = not configured |
| `umdc.clients.mercury.m2m.scopes` | `MERCURY_M2M_SCOPES` | `mercury:message:read,mercury:message:write` |

`umdc.clients.mercury.login.*` and `DIRECTORY_BACKEND_LOGIN_ALIAS` / `DIRECTORY_BACKEND_LOGIN_PASSWORD` no longer exist.

User-scoped mercury calls (campaigns, templates) are different: they need `Authorization: Bearer <Backbone opaque token>` and the end user's own
backbone `session-token`, passed through unchanged. mercury rejects a refresh token or a logged-out token there (401). Directory has no such call
today; the M2M token of this guide must never be used for them.

Tests: `MercuryTokensTest` (exchange, caches, 401 without loops, 429 and `Retry-After`, not configured, refused, unavailable),
`MercuryErrorsTest`, `MercuryVerificationServiceTest`, `AuthServiceImplTest`, `UserRegisterServiceImplTest`.
