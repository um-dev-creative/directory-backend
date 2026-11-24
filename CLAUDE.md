# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build (skip tests)
mvn -DskipTests package

# Full CI-aligned check
mvn -B -V -e clean verify

# Run all tests
mvn test

# Run a single test class
mvn -Dtest=com.umdc.controller.v1.api.directory.CampaignControllerTest test

# Run a single test method
mvn -Dtest=CampaignControllerTest#listCampaignsReturnsOk test

# Generate coverage report (opens at target/site/jacoco/index.html)
mvn clean test jacoco:report

# Run the application
java -jar target/directory-backend.jar
```

## Architecture

**Entry point:** `DirectoryBackendApplication.java` — `@EnableFeignClients`, scans `com.umdc.commons.services`, `com.umdc.directory`, `com.umdc.security`.

**Request flow:** Controller → Service → Repository

- `*Api.java` interfaces hold Swagger/OpenAPI annotations and default methods; `*Controller.java` are thin implementations.
- Service layer owns validation/status logic and returns `ResponseEntity` directly (not just domain objects).
- DTOs: `src/main/java/com/prx/directory/api/v1/to`
- Entities: `src/main/java/com/prx/directory/jpa/entity`
- Mappers: `src/main/java/com/prx/directory/mapper/*Mapper.java` (MapStruct, using shared config `com.umdc.commons.services.config.mapper.MapperAppConfig`)
- Constants: `src/main/java/com/prx/directory/constant` — prefer these over new string literals

**Filtering pattern:** `CampaignServiceImpl.list(...)` composes `CampaignFilterParser` → `CampaignSpecifications` → paged repository query. Campaign filters support legacy aliases (`q`, `category_id`, `business_id`, `start_date_*`, `end_date_*`) for backward compatibility.

**External integrations:**
- Backbone: `client/backbone/BackboneClient.java` + `BackboneFeignConfigurer`
- Mercury: `client/mercury/MercuryClient.java` + `MercuryFeignConfigurer`. Directory authenticates to Mercury **only** with the Backbone M2M
  exchange; Mercury does NOT accept an alias/password. Source: Mercury's own `application.yml` ("Backend applications do NOT log in to Mercury
  with a password: they exchange the Backbone M2M token of their managed client for Mercury's own M2M token (`POST /api/v1/auth/token`,
  `Authorization: Bearer <Backbone token>`)", commit 975e5c6):
  - `MercuryTokens` asks backbone-rest for the managed-client token (`BackboneManagedClient`, client credentials
    `MERCURY_M2M_CLIENT_ID` / `MERCURY_M2M_CLIENT_SECRET` / `MERCURY_M2M_SCOPES`), sends it as `Authorization: Bearer` to
    `MercuryAuthClient.exchange(...)` and gets Mercury's own token. No `session-token-bkd` header and no username/password are involved.
  - That token carries **no user**: use it only (as `session-token`) for `/api/v1/verification-code` and `/api/v1/mail`. Never retry-loop
    on `401` (one fresh token at most); honour `Retry-After` on `429`.
  - User-scoped Mercury calls (campaigns, templates) need `Authorization: Bearer <Backbone opaque token>` **and** the end user's backbone
    `session-token` header, passed through unchanged. Mercury rejects a refresh token or a logged-out token there (`401`).
- Auth: `services/AuthServiceImpl.java` + `security/SessionJwtServiceImpl.java`
- Kafka outbound email: `kafka/producer/EmailMessageProducerServiceImpl.java`

**Configuration:** Bootstrap-first (`src/main/resources/bootstrap.yml`) with Vault + Spring Cloud Config Server. Copy `default.env` for local environment variables.

**Tests:** JUnit 5 + Mockito. Test profile uses H2 in-memory DB with Vault/Config Server/Kafka/Security disabled (`src/test/resources/application-test.yml`). JaCoCo enforces 80% line coverage at bundle level.

## Key Conventions

- Keep backward compatibility for `/api/v1/*` contracts unless explicitly asked to break them.
- When changing API contracts, update both `*Api.java` annotations **and** `src/main/resources/api/index.yaml`.
- `CampaignServiceImpl.update(...)` uses optimistic-lock-style `lastUpdate` checks and returns `202 Accepted`.
- Preserve `DirectoryAppConstants.MESSAGE_HEADER` response/header semantics.
- Do not modify certificate/keystore assets (`src/main/resources/*.crt`, `keystore.jks`) unless explicitly requested.
- Treat `default.env` as sensitive — never commit secrets/tokens.
