# Changelog

All notable changes to this project will be documented in this file.
This project adheres (loosely) to "Keep a Changelog" and follows semantic versioning where practical.

## [Unreleased] - 2026-11-21 (project documentation and agent tooling)

### Added
- Project documentation under `docs/`: `PROJECT_DOCUMENTATION.md`, `TECHNICAL_DOCUMENTATION.md`, `ARCHITECTURE_DIAGRAMS.md`, business-analyst set (`COMPLETE_PROJECT_DOCUMENTATION_FOR_BA.md`, `BA_QUICK_REFERENCE.md`, `BA_VISUAL_SUMMARY.md`), `DOCUMENTATION_INDEX.md`, `DELIVERY_SUMMARY.md`, `BUSINESS_PROFILE_IMAGE_API.md`, `esp8266.md`.
- AI-assistant tooling under `.github/`: agents (including the orchestrator), skills, prompts, hooks and tool descriptions.
- Task backlog under `issues/` and `youtrack/`, plus `variables_reference.md` and `ISSUE-business-patch-contacttype-transient.md`.
- `docs/user-guide/project-documentation.md` and `docs/technical/project-documentation.md`: where each document lives.

### Changed
- `BUILD_VALIDATION_REPORT.md` moved to `docs/BUILD_VALIDATION_REPORT.md`; `README.md` and the agent/skill/tool files refreshed.
- Documentation only: no source, test or configuration file changes.
## [Unreleased] - 2026-11-21 (TLS bundle and session-token security, as in smartkin-backend / mercury / zerobound-rest)

### Changed
- **Certificates.** The TLS keystores now travel inside the jar: `pom.xml` copies only the `*.jks` of `certs/directory/` (git-ignored: `directory-rest.jks`, `umdc-truststore.jks`, `prx-internal-ca.crt`) onto the classpath. `spring.ssl.bundle.jks.directory-security`, `spring.cloud.vault.ssl`, `spring.cloud.config.tls`, `eureka.client.tls` and `umdc.security.truststore.location` read `${SSL_*_URL:classpath:${SSL_*_LOCATION}}`, so `SSL_KEYSTORE_URL` / `SSL_TRUSTSTORE_URL` (with a scheme) override the classpath names. `HttpClientConfig` resolves that location with a `ResourceLoader` (classpath, `file:` or a bare name). `umdc.security.keystore.*` is no longer read and was removed.
- **Authentication.** `DirectoryBackendApplication` excludes the library's Keycloak-style `SecurityConfig`; the new `SessionTokenSecurityConfig` is a stateless chain that lets `umdc.api.endpoint` (`/api/v1`) through and denies everything else except health probes and Swagger, and the library's `SessionJwtInterceptor` validates the `session-token` header. The OAuth2 resource-server auto-configurations and the `spring.security.oauth2.resourceserver` block (`AUTH_CERT_URI`) are gone; `umdc.api.endpoint` is new.
- **Image.** `Dockerfile` is two-stage (Spring Boot layers), no longer `COPY`s five `.crt` files from `src/main/resources`, and imports the internal CA only when the BuildKit secret `prx_internal_ca` is given; new `docker-entrypoint.sh`. Verified: the image builds, the CA lands in the JVM `cacerts`, and the jar carries `certs/directory/*` when present.

### Added
- `config/local.env` (non-secret, versioned) and `secrets/local.env.example`; `.gitignore` now ignores `certs/` and `secrets/*.env`.
- `SessionTokenSecurityConfigTest` (4 tests) and `SslLocationPropertiesTest` (3 tests); suite: 591 tests, method coverage 93.1 %. Guides: `docs/user-guide/tls-and-session-security.md`, `docs/technical/tls-and-session-security.md`.

### Notes
- **Action needed:** put `directory-rest.jks` (the service keystore, not in the repository), `umdc-truststore.jks` and `prx-internal-ca.crt` in `certs/directory/` before building the jar; keystore passwords go in `secrets/local.env` or Vault, never in the repository. Remove `AUTH_CERT_URI` from the environment.
- `config/local.env` matches the real files: `directory-rest.jks` is JKS, `umdc-truststore.jks` is PKCS12 (`SSL_TRUSTSTORE_TYPE=PKCS12`). Both open with the default `changeit` password: rotate it before production.
- Not run against a live Vault / Config Server / Backbone: the TLS handshake and the interceptor with real tokens are untested here.

---

## [Unreleased] - 2026-11-21 (com.umdc migration)

### Changed
- **Breaking for operators.** Base package renamed `com.prx.directory` → `com.umdc.directory` (main and test sources), Maven `groupId` `com.prx` → `com.umdc`, and the shared libraries now come from the `com.umdc` group (`commons`, `commons-services`, `security-oauth`, version `0.0.1`).
- **Platform upgrade:** Spring Boot `3.5.8` → `4.0.6`, Spring Cloud `2025.0.1` → `2025.1.1`, JUnit `5.14.1` → `6.0.3`, and **Java `21` → `25`** (the `com.umdc` `security-oauth` artifact is compiled for Java 25). `Dockerfile` (`amazoncorretto:25-alpine`) and the `build`, `ci` and `qodana` workflows follow.
- **Configuration keys moved** from `prx.*` to `umdc.*` (`umdc.security.jwt`, `umdc.security.auth.clients`, `umdc.security.keystore|truststore`, `umdc.logging.trace.enabled`, `umdc.database-cert`, `umdc.db.ssl.*`, `umdc.directory.*`, `umdc.verification.code.template.id`, `umdc.clients.mercury.m2m.*`). The roles variable is now `APP_CLIENT_ROLES` (was `AUTH_ROLE_ID`) and the SSL stores use `SSL_KEYSTORE_*` / `SSL_TRUSTSTORE_*`. **Action needed:** update Vault / Config Server before deploying. See `docs/technical/umdc-migration.md` and `docs/user-guide/umdc-migration.md`.
- `BackendFeignConfigurer` now requests tokens with a `RestTemplate` that uses the `directory-security` SSL bundle; `DataSourceSslConfig` no longer declares `@EntityScan` (Spring Boot 4 scans from the application package).

### Added
- `RedisConfig` (`LettuceConnectionFactory` from `DataRedisProperties`, supports `redis://` / `rediss://` URLs, SSL, username/password, default 10 s timeouts) so the timezone cache keeps working on Spring Boot 4.
- `HttpClientConfig` and `CertificateSecurityException` in `com.umdc.directory.config`; new `banner.txt`.

### Tests
- `RedisConfigTest` (4 tests). Mockito-based tests migrated to `MockitoExtension`. Full suite: 584 tests, method coverage 93.1 %.

---

## [Unreleased] - 2026-11-13

### Changed
- **Breaking for operators.** Directory no longer logs in to mercury with an alias and a password. It asks backbone-rest for the Backbone M2M token of its own managed client (`POST /api/v1/managed-clients/token`, scopes `mercury:message:read` and `mercury:message:write`), sends it as `Authorization: Bearer` to mercury's `POST /api/v1/auth/token` and sends the M2M token mercury answers (tied to the Directory application) in `session-token`. New `MercuryTokens` caches both tokens until a minute before they expire and retries once with fresh tokens on a 401; `MercuryAuthClient` and `BackboneManagedClient` are Feign clients without the OAuth interceptor. `MercuryClient.token` and the `session-token-bkd` header towards mercury are gone, and `UserRegisterServiceImpl.confirmCode` no longer reads the user from backbone (it sent that user's alias and password to mercury). **Action needed, in this order:** (1) deploy backbone-rest, `security-oauth` and mercury with the Backbone-token exchange; (2) create Directory's managed client in Backbone, tied to the Directory application, with both scopes; (3) put `MERCURY_M2M_CLIENT_ID` and `MERCURY_M2M_CLIENT_SECRET` in Vault (optional `MERCURY_M2M_SCOPES`); (4) deploy this. See `docs/technical/mercury-m2m-auth.md` and `docs/user-guide/mercury-m2m-auth.md`.

### Tests
- Method coverage of the whole project went from 67.75 % (437/645) to 93.95 % (606/645). New: `TransferObjectsSmokeTest` (every record builds, prints and keeps `equals`/`hashCode` consistent), `MapperImplementationsTest` (every MapStruct method runs on fully populated arguments), `DefaultInterfaceMethodsTest` (every default method of the service/API interfaces answers 501 or throws `UnsupportedOperationException`), plus `SessionJwtServiceImplTest`, `KafkaProducerConfigTest` and `MercuryTokensTest`. The first three discover their classes from the classpath, so new records, mappers and interfaces are covered without editing them; their fixtures are in `src/test/java/com/prx/directory/support`. They are smoke tests: they prove nothing throws, not that a mapping is semantically right.

---

## [Unreleased] - 2026-04-16 (Redis timezone cache)

### Added
- **Redis caching layer for timezone queries** — implements `TimezoneCacheService` / `TimezoneCacheServiceImpl` to serve the timezone list from Redis (`timezones:all:v1`) instead of hitting PostgreSQL on every request.
  - `TimezoneCacheService` — new interface exposing `getAllTimezones()`, `refreshTimezones()`, and `invalidateTimezones()`.
  - `TimezoneCacheServiceImpl` — `@Service` backed by `StringRedisTemplate`. Implements:
    - Cache-hit path: deserializes JSON from Redis and returns without touching the DB.
    - Cache-miss path with stampede protection: acquires a short-lived Redis lock (`timezones:lock`, default 5 s TTL) before loading from PostgreSQL. Other instances that cannot acquire the lock fall back to a direct DB read.
    - Redis outage: any `DataAccessException` while reading the cache is logged as `WARN` and the list is read straight from PostgreSQL, so `/api/v1/timezones` keeps working without Redis.
    - `refreshTimezones()` — forces a DB reload and overwrites the cached value (TTL 24 h by default, fixed; see `TIMEZONE_CACHE_DEFAULT_TTL_SECONDS`).
    - `invalidateTimezones()` — deletes the cache key so the next read triggers a fresh DB load.
    - Structured `DEBUG` / `INFO` / `WARN` / `ERROR` logging for all cache lifecycle events.
  - `TimezoneServiceImpl` — updated to accept `TimezoneCacheService` via constructor injection. `findAll()` now delegates to `TimezoneCacheService#getAllTimezones()` instead of calling `TimezoneRepository` directly. `getTimezonesPageable(Pageable)` is unchanged.
  - `DirectoryAppConstants` — added constants: `TIMEZONE_CACHE_KEY`, `TIMEZONE_CACHE_LOCK_KEY`, `TIMEZONE_CACHE_DEFAULT_TTL_SECONDS`, `TIMEZONE_CACHE_LOCK_TTL_MS`.
  - `spring-boot-starter-data-redis` dependency added to `pom.xml` (version managed by Spring Boot BOM).
- **Documentation** — `docs/user-guide/timezone-redis-cache.md` and `docs/technical/timezone-redis-cache.md`.
- **Unit tests** — 11 new / updated test cases:
  - `TimezoneCacheServiceImplTest` (8 tests) — covers Redis unavailable (DB fallback), cache-hit (no DB call), cache-miss with lock acquired (DB load + Redis write), cache-miss with lock not acquired (fallback to DB, no Redis write), bad JSON deserialization returning empty list, `refreshTimezones` writing to Redis, `invalidateTimezones` deleting the key, and the invalidate-then-get flow triggering a reload.
  - `TimezoneServiceImplTest` (3 tests, updated) — updated constructor call to inject `TimezoneCacheService` mock; `findAll` tests now verify delegation to the cache service and assert no repository interaction.

### Changed
- `TimezoneServiceImpl` constructor signature extended with `TimezoneCacheService` parameter (no API contract change).

---

## [Unreleased] - 2026-04-12


### Added
- **Business profile image upload via Cloudflare R2** — new `POST /api/v1/businesses/images/{businessId}` endpoint stores the profile image in R2 and persists the public URL in the business record.
  - `BusinessProfileImageService` — local interface extending `ImageService`; adds `uploadForBusiness(UUID, byte[], String)` method.
  - `BusinessProfileImageServiceImpl` — `@Service` implementing `BusinessProfileImageService`. Injects `CloudflareR2StorageClient` and `BusinessRepository`. Implements:
    - `upload(ImageUploadRequest)` — generic upload; auto-generates key under `businesses/` prefix when `objectKey` is null.
    - `uploadForBusiness(UUID, byte[], String)` — scoped upload; generates key as `businesses/{businessId}/{uuid}.{ext}`, uploads bytes to R2, and saves the public URL in `business.profile_image_ref`. Returns `404` when the business is not found.
    - `getReference(String)` — returns the public URL without downloading bytes.
  - `BusinessProfileImageController` — `@RestController` at `/api/v1/businesses/images` implementing `ImageApi`. Overrides `upload`, `getReference`, and adds the new business-scoped `POST /{businessId}` endpoint.
- **Flyway migration `V3`** — `V3__add_profile_image_ref_to_business.sql` adds `profile_image_ref VARCHAR(512) NULL` to `directory_site.business`.
- **`BusinessEntity`** — new `profileImageRef` field (`@Column(name = "profile_image_ref")`) with getter and setter.
- **`BusinessTO`** — new `profileImageRef` record component; `toString()` updated.
- **`BusinessMapper`** — added `@Mapping(target = "profileImageRef", source = "profileImageRef")` to `toBusinessTO`.
- **Unit tests** — 18 new test cases across two new test classes:
  - `BusinessProfileImageServiceImplTest` — covers `upload` with explicit and generated keys, extension mapping for all supported MIME types (`image/png`, `image/webp`, `image/gif`, default → `.jpg`), `uploadForBusiness` happy path and 404 branch, and `getReference`.
  - `BusinessProfileImageControllerTest` — covers `getService`, `upload` (explicit and null key), `getReference`, `uploadForBusiness` with explicit content type, null content type defaulting to `image/jpeg`, and 404 propagation.

### Changed
- Updated existing `BusinessTOTest`, `BusinessEntityTest`, `BusinessServiceImplTest` (×3), and `FavoriteServiceImplGetFavoritesTest` (×2) constructor calls to include the new `profileImageRef` component.

---

## [Unreleased] - 2026-02-16

### Added
- Unit tests for `ProductServiceImpl` covering:
  - `create(...)` happy path and null-request validation.
  - `linkProductToBusiness(...)` happy path and validation/error branches (null request, null entity id).
  - `findByBusinessId(...)` branches: invalid page/perPage inputs, null businessId, business-not-found, branch when `active` is `null` (uses `findByBusiness`), and branch when `active` is specified (uses `findByBusinessAndActive`) with filtering of null product entries.
- Unit tests for `ProfileImageServiceImpl.save(...)` covering OK and non-OK backend responses.
- `@DisplayName` annotations added or standardized on multiple service test classes to improve test reports (human-readable test names).
- `README.md` — new **Requirements** section placed above the "Getting started (developer)" section describing developer prerequisites (JDK 21, Maven, optional Docker, DB/Kafka/Vault notes, `.env` guidance, recommended machine resources).

### Changed
- Improved test reporting by adding class-level and method-level `@DisplayName` where appropriate in existing tests under `src/test/java/com/prx/directory/api/v1/service`.

### Test helpers (test-only)
- To allow focused local test compilation and runs in this repository checkout, minimal test-only stubs were added under `src/test/java/com/prx/security/*`.
  - These files are small, purpose-built test stubs (interfaces/records) used to satisfy references from existing tests to `com.prx.security` types that are not included in this repository. They are NOT production code and should be removed or replaced with the real `com.prx.security` module once that dependency is available in your build environment.

### Notes for reviewers / maintainers
- The added unit tests increase service-layer coverage for `ProductServiceImpl` and `ProfileImageServiceImpl`. The tests were run locally for the focused suite and passed. Example focused test run used:

```powershell
# run only ProductServiceImpl tests
cd C:\projects\directory-backend
.\mvnw -DskipTests=false -Dtest=ProductServiceImplTest test
```

- Running the full test suite (`.\mvnw test`) may require the real `com.prx.security` module or other internal modules; until those are present, the included test-only stubs under `src/test/java` are intended to unblock focused local verification. Prefer adding the real modules as dependencies in `pom.xml` and removing the stubs when available.

- The README change is non-breaking and helps new developers prepare their environment before following the "Getting started" steps.

### Potential follow-ups (recommended)
- Replace test-only stubs with the real `com.prx.security` module (add it to `pom.xml` or include it as a multi-module project dependency) and remove the temporary files under `src/test/java/com/prx/security`.
- Add more targeted unit tests for other service implementations (e.g., `FavoriteServiceImpl`, `CampaignServiceImpl`) to further raise coverage.
- Run CI full-suite and confirm coverage metrics; update the changelog with a release entry when cutting a release.

---

### How to reproduce locally (quick commands)
```powershell
# run focused ProductServiceImpl test
cd C:\projects\directory-backend
.\mvnw -DskipTests=false -Dtest=ProductServiceImplTest test

# run new ProfileImageServiceImpl test
.\mvnw -DskipTests=false -Dtest=ProfileImageServiceImplTest test

# run full test suite (may require additional modules / dependencies)
.\mvnw test
```

If you'd like, I can also:
- Add a release section (e.g., `## [0.0.1] - 2026-02-16`) and tag a release commit.
- Open a PR with this changelog and the accompanying commits (I can create the branch and PR if you want me to push changes remotely).

---

*This changelog entry was generated automatically as part of the recent test and doc work performed on the `directory-backend` repository.*

