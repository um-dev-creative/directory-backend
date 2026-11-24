# Session token and CORS security (user guide)

## What changes for API clients

- The `session-token` header must be a token issued by this service and still valid. A forged, unsigned, altered or expired token is answered `401` on every endpoint that identifies the user (favorites, favorite lists, saved searches) and no longer counts as the business owner on click events.
- Browsers: a web application can call the API only from the origins listed by the operator. Any other origin receives `403` on the preflight and on the request. Mobile apps and server-to-server clients are not affected.
- Scripts in an allowed web application can read the response headers `message`, `message-error`, `Retry-After` and `X-RateLimit-Limit`.

## Operator settings

| Variable | Property | Default | Meaning |
| --- | --- | --- | --- |
| `UMDC_CORS_ALLOWED_ORIGINS` | `umdc.cors.allowed-origins` | empty (no cross-origin) | Comma list of exact origins, no `*` (startup fails) |
| | `umdc.cors.allowed-methods` | `GET,POST,PUT,PATCH,DELETE,OPTIONS` | Allowed methods |
| | `umdc.cors.allowed-headers` | `session-token,Content-Type,Accept,Authorization` | Allowed request headers |
| | `umdc.cors.max-age-seconds` | `1800` | Preflight cache |
| | `umdc.security.jwt.verify-signature` | `true` | Verify signature and expiry of the session token |
| | `umdc.security.jwt.min-key-bytes` | `32` | Minimum length of the decoded `APP_TOKEN_SECRET` |

Suggested `UMDC_CORS_ALLOWED_ORIGINS` per environment (adjust to the real hosts): local `http://localhost:3000`; dev and QA the front-end host of that environment; production only the production front-end host. Never reuse a list across environments.

## Before deploying

- `APP_TOKEN_SECRET` must be Base64 and decode to 32 bytes or more (for example `openssl rand -base64 48`). A shorter secret stops the application at startup. Changing it logs every user out.
- Leave `verify-signature` at `true`. See the technical guide for the only reason to turn it off.
