# Moving Directory to the `com.umdc` platform (user guide)

Directory now runs on Java 25 / Spring Boot 4 and uses the `com.umdc` shared libraries. The public API (`/api/v1/*`) does not change.
Operators must update configuration **before** deploying.

## Checklist for operators

1. Use a **Java 25** runtime (the Docker image is now `amazoncorretto:25-alpine`).
2. In Vault / Config Server rename the keys:

| Before | Now |
| --- | --- |
| `prx.jwt.*` | `umdc.security.jwt.*` |
| `prx.auth.clients[*]` | `umdc.security.auth.clients[*]` |
| `prx.logging.trace.enabled` | `umdc.logging.trace.enabled` |
| `prx.database-cert` | `umdc.database-cert` |
| `app.db.ssl.*` | `umdc.db.ssl.*` |
| `prx.directory.application-id`, `prx.directory.role-id` | `umdc.directory.application-id`, `umdc.directory.role-id` |
| `prx.verification.code.template.id` | `umdc.verification.code.template.id` |
| `prx.clients.mercury.m2m.*` | `umdc.clients.mercury.m2m.*` |
| env `AUTH_ROLE_ID` | env `APP_CLIENT_ROLES` |

3. Provide `SSL_KEYSTORE_LOCATION/PASSWORD/TYPE` and `SSL_TRUSTSTORE_LOCATION/PASSWORD/TYPE` (values only in Vault or the environment, never in the repository).
4. Redis (timezone cache): `spring.data.redis.host/port/password/ssl.enabled`, or a single `spring.data.redis.url` (`redis://` or `rediss://`).

## What to expect

| Situation | Result |
| --- | --- |
| Old `prx.*` keys still in Vault | The application fails at start-up or ignores the value; rename them |
| Old Java 21 runtime | Fails with `UnsupportedClassVersionError` |
| Everything migrated | Same endpoints and responses as before |
