# Authentication towards mercury (technical guide, MER-11)

Directory logs in to mercury with its registered alias and password alone. The token is cached and used only for
`/api/v1/verification-code` and `/api/v1/mail`.

```mermaid
sequenceDiagram
    participant D as Directory (MercuryTokens)
    participant M as mercury
    D->>M: POST /api/v1/auth/token {alias, password}
    alt 200
        M-->>D: token (no user)
        D->>M: POST /api/v1/verification-code, session-token: token
        M-->>D: 2xx / 4xx
    else 401
        M-->>D: 401
        Note over D: no retry; no call to mercury for 1 minute (503 to the caller)
    else 429
        M-->>D: 429 Retry-After: n
        Note over D: nothing sent for n seconds; 429 + Retry-After to the caller
    end
```

| Piece | Role |
| --- | --- |
| `client/mercury/MercuryAuthClient` | Feign client of `POST /api/v1/auth/token` (`AuthRequest`). Its configuration (`MercuryAuthFeignConfig`) only sets timeouts: no OAuth Bearer is added. |
| `client/mercury/MercuryTokens` | Logs in, caches the token until one minute before `exp` (5 minutes when `exp` cannot be read), thread-safe. `withToken(call)` runs a call; a 401 of the call drops the token and repeats it **once**; a 401 of the login is never retried; a 429 (login or call) records the `Retry-After` and every later call fails fast with `MercuryThrottledException` until it passes. |
| `client/mercury/MercuryClient` | Calls that carry Directory's token in `session-token` (`confirmCode`, `isVerificationCodeDone`). `MercuryFeignConfigurer` adds the OAuth Bearer and the timeouts (connect 3 s, read 5 s). |
| `client/mercury/MercuryErrors` | `MercuryThrottledException` becomes 429 + `Retry-After`; `MercuryUnavailableException` (not configured, login refused, 5xx, timeout) becomes 503. Both carry `message-error` and never a credential. |
| `api/v1/service/MercuryVerificationService` | Thin layer: `sendCode` (Kafka e-mail that mercury delivers), `verifyCode` (`POST /verification-code`), `isVerified` (`latest-status`). Never logs a code or a token. |

Properties: `umdc.clients.mercury.login.alias` / `umdc.clients.mercury.login.password`; when absent, the environment variables
`DIRECTORY_BACKEND_LOGIN_ALIAS` / `DIRECTORY_BACKEND_LOGIN_PASSWORD` are read. Empty = not configured (the context still starts; calls answer 503).

User-scoped mercury calls (campaigns, templates) are different: they need `Authorization: Bearer <Backbone opaque token>` and the end user's own
backbone `session-token`, passed through unchanged. mercury rejects a refresh token or a logged-out token there (401). Directory has no such call
today; the token of this guide must never be used for them.

Follow-up outside this change (the YAML is not part of it): the `umdc.clients.mercury.m2m` block of `bootstrap.yml` is no longer read and can be
removed; `BackboneManagedClient`, `ManagedClientToken` and `ManagedClientTokenRequest` were deleted.

Tests: `MercuryTokensTest` (login, cache, 401 without loops, 429 and `Retry-After`, not configured, unavailable), `MercuryErrorsTest`,
`MercuryVerificationServiceTest`, `AuthServiceImplTest`, `UserRegisterServiceImplTest`.
