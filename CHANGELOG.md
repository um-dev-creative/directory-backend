## [Unreleased] - 2026-11-23 (API contract: Phase 2)

### Changed
- `src/main/resources/api/index.yaml` documents Phase 2: products `PATCH`/`PUT`/`DELETE /products/{productId}`, `include_inactive` and the new `price`, `currency`, `imageRef`, `businessId` fields of `POST /products`; campaigns `deactivate`, `reactivate`, `publish`, the `type` field, the `status` and `type` filters and the optional `session-token`; `/users/me/saved-searches` (CRUD and replay) and `/users/me/last-search`; business click events `POST /businesses/{businessId}/events` and `GET .../events/summary`. Schemas, status codes, the `message-error` header and the `session-token` requirement follow the Phase 0 and Phase 1 entries.
- The example password in `/auth/session-token` and `/auth/token` is replaced by the placeholder `<password>`.

### Added
- Guides: `docs/user-guide/api-contract-f2.md`, `docs/technical/api-contract-f2.md` (Mermaid diagrams).

### Notes
- Documentation only: no code, schema or configuration change.

# Changelog

All notable changes to this project will be documented in this file.
This project adheres (loosely) to "Keep a Changelog" and follows semantic versioning where practical.

## [Unreleased] - 2026-11-23 (API contract: Phase 0 and Phase 1)

### Changed
- `src/main/resources/api/index.yaml` now documents Phase 0: required `session-token` and `401/403/404` answers on business create/update, business images (client `objectKey` ignored), product create/link and campaign create/update; favourites `sort`, `type`, `size` (max 100, 0-based page) and the `409`/`201` duplicate and reactivation rules; global rate limiting (`429` with `Retry-After` and `X-RateLimit-Limit`, as a reusable response added to every operation); Swagger/api-docs open only with `umdc.docs.enabled`.
- Phase 1 (from the open PRs #126, #127, #128, not merged yet): `GET /businesses/nearby`, `GET /businesses/map` (GeoJSON), opening hours `GET/PUT .../hours` and `GET .../hours/status`, and `POST /geocode`.
- Contract defects fixed without changing any real route: fixed-UUID paths became `{businessId}`, `{campaignId}`, `{favoriteId}`; empty `responses` filled with the real codes; `GET /favorites` lost its bogus request body and now lists its query parameters; `GET /products/business/{businessId}` added; reusable schemas and responses added under `components`.
- The favourite delete route is documented as `DELETE /favorites/{favoriteId}` (it was listed without the id, a route that does not exist).

### Added
- Guides: `docs/user-guide/api-contract-f0-f1.md`, `docs/technical/api-contract-f0-f1.md` (Mermaid diagrams).

### Notes
- Documentation only: no code, schema or configuration change. The Phase 1 endpoints exist in the contract before they exist in `development`.
- `PUT /favorites/{id}` is a `PATCH` in the code and the contract follows the code.
## [Unreleased] - 2026-11-23 (analysis documents moved out of the repository)

### Removed
- `docs/technical/mvp-product-analysis.md` and `docs/user-guide/mvp-product-analysis.md`. Analysis, planning and runbook documents are not versioned in the repository: only the CHANGELOG and the user and technical guides of each feature are. The content is kept outside the repository.

### Changed
- `docs/technical/f0-platform.md` and `docs/technical/f0-product-campaign-owner-checks.md` no longer link to the removed analysis.

### Notes
- Documentation only: no code, API, schema or configuration change.
- The earlier CHANGELOG entry "MVP product and architecture analysis" is kept as history; the guides it names were removed here.

## [Unreleased] - 2026-11-23 (MVP product and architecture analysis)

### Added
- Analysis of the first phase (MVP, Toronto / GTA) for businesses and consumers: current state verified in the code, functional requirements RF-01 to RF-12 with MoSCoW priority, non-functional requirements, geo search design (`nearby`, `map`, `geocode`, saved searches), proposed migration V6, map provider comparison, test strategy, marketing capabilities and gaps, 12-week plan and risk register.
- Guides: `docs/user-guide/mvp-product-analysis.md`, `docs/technical/mvp-product-analysis.md` (Mermaid diagrams).

### Notes
- Documentation only: no code, API, schema or configuration change. The proposed V6 script and endpoints are not implemented.
- Findings verified in the code: no owner check in campaign and product writes, business `create` reads `userId` from the body (`BusinessServiceImpl.java:89`), Swagger is `permitAll`, favourites `sort` answers `501`. Findings from a reviewer and not re-verified are marked as such in the technical guide.
- `issues/created/TASK-002` (campaign update "not implemented") and `TEST_COVERAGE_SUMMARY.md` (`com/prx` paths) are stale; not changed here.
- 664 tests pass; JaCoCo method coverage 93.25 % (760 of 815). `mvn test` alone does not hand the agent to Surefire here (the `jacoco.argLine` property stays empty), so coverage was measured by passing `-Djacoco.argLine=-javaagent:<jacoco agent>=destfile=target/jacoco.exec` and then `jacoco:report`.

## [Unreleased] - 2026-11-22 (container images: JVM and native)

### Added
- `Dockerfile.native`: GraalVM native image. Compiles from source in the container (`./mvnw -Pnative package`, profile inherited from `spring-boot-starter-parent`) and runs on `debian:bookworm-slim` as a non-root user with the same port and `HEALTHCHECK` as the JVM image. The private Maven settings and the optional internal CA are BuildKit secrets, never stored in a layer; the truststore is the JDK `cacerts` plus the CA.
- `docker-entrypoint.native.sh` (native options only) and `docker-build.sh jvm|native|all`, which builds `lamata/directory-backend:<version>` (`Dockerfile`) and `:<version>-native` (`Dockerfile.native`).
- `config/native-aot.env`: placeholder values (no credentials) so Spring AOT can load `bootstrap.yml` during the native build.
- `.dockerignore`: keeps `secrets/`, `*.env`, `config/` and `.git` out of the build context; only `target/directory-backend.jar` of `target/` is sent.
- Guides: `docs/user-guide/docker-images.md`, `docs/technical/docker-images.md`.

### Notes
- **Native runtime is not verified.** The image builds and the executable starts, but with placeholder configuration it stops at `No spring.config.import property` (Config Server) and, with Config Server/Vault disabled, at `no ServletWebServerFactory bean` (AOT conditions are fixed at build time with `config/native-aot.env`). It has not been run against real Config Server, Vault, PostgreSQL and Redis. The JVM image is the supported one until that is checked.
- `Dockerfile.native` uses `native-image-community:25-ol9`: the `:25` tag aborts with `Fatal glibc error: CPU does not support x86-64-v3` on hosts without that instruction set (amd64 emulation, older CPUs).
- The native image needs about 8 GB of memory for Docker and a long build; the JVM image is unchanged.

## [Unreleased] - 2026-11-23 (F0-A1 business and image owner checks)

### Security
- `PATCH /api/v1/businesses/{id}` now requires the `session-token` header and that the uid of the token owns the business: `401` without a valid token, `404` when the business does not exist, `403` when the caller is not the owner.
- `POST /api/v1/businesses` takes the owner from the uid of the token. A `userId` in the body that differs from the token answers `403`; a missing `userId` is accepted and replaced by the token user (backward compatible). No or invalid token answers `401`.
- `POST /api/v1/businesses/images/{businessId}` validates ownership with the token (`401`, `404`, `403`).
- `POST /api/v1/businesses/images` (generic upload) requires a valid token (`401`) and ignores any client `objectKey`; the storage key is always generated by the server, so callers cannot overwrite arbitrary objects.

### Changed
- `BusinessService.create/update` and `BusinessProfileImageService.uploadForBusiness` receive the session token.
- Tests: owner matrix (owner, other user, no token, unknown business) for business update/create and image upload; `TestTokens` test helper.
- Docs: `docs/user-guide/f0-business-owner-checks.md`, `docs/technical/f0-business-owner-checks.md`.

## [Unreleased] - 2026-11-22 (startup banner versions)

### Added
- The startup banner (`banner.txt`, same lettering as `backbone-rest`) now also prints `Spring Cloud <version> | UMDC libraries <version>` after the Spring Boot version. The versions come from `src/main/resources/build-versions.properties`, filtered by Maven (`@spring-cloud.version@`, `@com.umdc.commons.version@`; only that file is filtered) and imported by `spring.config.import` in `bootstrap.yml`. A missing value prints `n/a` and never stops the startup.
- `BannerVersionsTest` (3): the filtered file has real versions, the banner and `bootstrap.yml` reference it, and the banner renders with no unresolved placeholder.
- Guides: `docs/user-guide/startup-banner.md`, `docs/technical/startup-banner.md`.

### Notes
- Verified with a real Spring startup (bootstrap context off). Not verified with the bootstrap context enabled; there the worst case is `n/a`.
- 664 tests pass; JaCoCo method coverage 93.25 %.

## [Unreleased] - 2026-11-23 (F0-A2 product and campaign owner checks)

### Security
- `POST /api/v1/products`, `POST /api/v1/products/link`, `POST /api/v1/campaigns` and `PATCH /api/v1/campaigns/{id}` now require the `session-token` header and verify the owner of the affected business (RF-01, part 2).
- Responses: `401` without a valid token, `404` when the business or campaign does not exist, `403` when the token user is not the owner. Public reads (`GET` campaigns, `GET /products/business/{id}`) are unchanged and need no token.
- Campaign create with an unknown business now answers `404` instead of `400`.
- Product create has no business in its payload, so it requires a token whose user owns at least one business (`403` otherwise).
- Campaign PATCH moving a campaign to another business also requires owning the target business. Optimistic locking (`lastUpdate`, `409`) and `202 Accepted` are preserved.

### Tests
- Added owner matrix tests (owner, other user, no token, missing business or campaign) and a test token helper; existing tests adapted to the new signatures.

### Documentation
- Added user and technical guides: `docs/user-guide/f0-product-campaign-owner-checks.md` and `docs/technical/f0-product-campaign-owner-checks.md`.

## [Unreleased] - 2026-11-22 (dependency update)

### Changed
- **Spring Boot 4.0.6 -> 4.1.2** and Spring Cloud 2025.1.1 -> 2025.1.4, aligned with `backbone-rest` (and `mercury` / `zerobound-rest` where backbone does not define the property).
- UMDC libraries: `commons` 0.0.1 -> 0.0.4, `commons-services` 0.0.1 -> 0.0.3, `security-oauth` 0.0.1 -> 0.0.3.
- Libraries: springdoc 2.6.0 -> 3.1.0 (maven plugin 1.4 -> 1.5), jjwt 0.12.3 -> 0.13.0, PostgreSQL driver 42.7.7 -> 42.7.14, Gson 2.14.0, SnakeYAML 2.6, org.json 20260814, commons-lang3 3.21.0, plexus-utils 4.1.0, ASM 9.10.1, OpenRewrite plugin 6.46.1 / recipes 6.37.1, JUnit 6.1.3, Mockito 5.24.0.
- Build plugins: compiler 3.15.0, javadoc 3.12.0, JaCoCo 0.8.15, surefire 3.5.6.

### Security
- Overrides of the Boot BOM, as in `backbone-rest`: hibernate-validator 9.1.6.Final, Jackson 2.21.8 and 3.1.8, logback 1.6.6 and Apache `httpclient` 4.5.14 (GHSA-7r82-7xv7-xcpj, transitive via `eureka-client`). Tomcat already resolves to 11.0.27 with Boot 4.1.2.

### Documentation
- Guides: `docs/user-guide/dependency-update.md`, `docs/technical/dependency-update.md`.
- Fixed stale version tables in `docs/PROJECT_DOCUMENTATION.md`, `docs/TECHNICAL_DOCUMENTATION.md` and `docs/COMPLETE_PROJECT_DOCUMENTATION_FOR_BA.md` (they said Java 21, Spring Boot 3.5.8, Cloud 2025.0.1, jjwt 0.12.3 and springdoc 2.6.0).

### Notes
- No API, database or configuration change. 661 tests pass; JaCoCo method coverage 93.25 % (760 of 815).
- Not run against Vault, Config Server, Kafka or a real PostgreSQL: smoke-test in a real environment before releasing.
- Supersedes Dependabot PR #105 (plexus-utils), now at 4.1.0.

## [Unreleased] - 2026-11-23 (F0-B favorites)

### Fixed
- `GET /api/v1/favorites` no longer answers `501` when `sort` is sent. `sort=created_at|name[,asc|desc]` is implemented (a bare field sorts ascending; the default is `created_at,desc`). An unknown field, direction or format answers `400`, and so do an unknown `type` and `size < 1`.
- Pagination now runs in the database (`UserFavoriteRepository.findPageByUser`, one query plus a count) instead of loading every favorite and slicing it in memory. The page stays 0-based (a negative `page` is treated as `0`) and `size` is capped at 100. The combined result keeps the order stores, then products, then offers.
- `PUT /api/v1/favorites/{id}` resolves the target according to the type of the favorite (store, offer or product) instead of always looking up a product. The owner check now runs before the target lookup, and moving a favorite onto a target the user already has active answers `409`.
- Creating a favorite that was soft-deleted reactivates it (`201`, `deleted_at` and `deleted_by` cleared) instead of answering `409`; only an active duplicate answers `409`. Setting `active=true` through update clears the soft-delete fields as well.
- A unique-constraint violation from a concurrent duplicate (`DataIntegrityViolationException`) is mapped to `409` on create and update (`saveAndFlush`; both methods are no longer wrapped in a class-level transaction so the violation does not poison it).

### Added
- `UserFavoriteRepositoryTest` (real queries on H2) and `FavoriteServiceImplF0Test`; the old `501` and in-memory pagination tests were replaced.
- User guide `docs/user-guide/f0-favorites.md` and technical guide `docs/technical/f0-favorites.md`, which includes the proposed partial unique index and `CHECK num_nonnulls` script for the migrations flow (no DDL is applied by this change).

## [Unreleased] - 2026-11-21 (business view statistics)

### Added
- **View counter without personal data.** New table `directory_site.business_view_daily` (migration `V5__create_business_view_daily.sql`, applied by hand): one row per business and UTC day with a number of views. No user id, IP address or user agent is stored anywhere.
- `POST /api/v1/businesses/{businessId}/views` counts one view for today (UTC) and answers `204`. A view by the owner of the business is not counted; an unknown business is `404`. The increment is one atomic `UPDATE`; the first view of a day inserts the row and a concurrent first view falls back to the `UPDATE` (each step in its own transaction).
- `GET /api/v1/businesses/{businessId}/stats?days=30` (owner only: `uid` of the session token; `401`/`403`/`404`; `days` 1..365 else `400`): `totalViews`, `viewsToday`, `viewsLast7Days` (independent of a shorter window) and `daily`, one entry per day with zeros included, oldest first.
- `BusinessRepository.findOwnerId` reads the owner without loading the business. `src/main/resources/api/index.yaml` documents both endpoints. Guides: `docs/user-guide/business-statistics.md`, `docs/technical/business-statistics.md`.

### Tests
- `BusinessStatsServiceImplTest` (12, fixed clock: the day is the UTC one even when the server zone is already the next day), `BusinessViewDailyRepositoryTest` (5, `@DataJpaTest` on H2: the atomic increment, the range query and the composite key), `BusinessStatsRoutesTest` (5). Adds the test dependency `spring-boot-starter-data-jpa-test`.

### Notes
- **Action needed:** apply `V5` before deploying (and `V2`-`V4` if pending); Flyway is not configured. Not run on a real PostgreSQL here.
- Without personal data the counter cannot tell unique visitors apart: it counts page views, and a visitor who reloads is counted again. The BFF rate limit is the only brake.

## [Unreleased] - 2026-11-23 (F0-C platform: docs lock, rate limiting, Flyway, coverage)

### Added
- **Rate limiting** of `/api/v1/**`: in-memory token buckets per client IP and per user id (`uid`/`sub` of the `session-token`), `429` with `Retry-After` and `X-RateLimit-Limit`; `/actuator/health` is never limited. Pluggable `RateLimitStore` interface for a Redis store later. Properties `umdc.ratelimit.enabled` (default true; false in the test profile), `ip-per-minute` (120), `user-per-minute` (300), `trust-forwarded-header` (false).
- **Flyway** (`spring-boot-starter-flyway`, `flyway-database-postgresql`), disabled by default (`spring.flyway.enabled=false`, env `SPRING_FLYWAY_ENABLED`), with `baseline-on-migrate=true`, `baseline-version=5`, `locations=classpath:db/migration`. Numbering rule documented: never edit existing scripts; `V6` (location hours) and `V7` (saved_search / favorite_list) are reserved for Phase 1.
- Tests: `InMemoryRateLimitStoreTest`, `RateLimitFilterTest`, `RateLimitConfigTest`, `SessionTokenSecurityConfigDocsClosedTest`, `PlatformDefaultsTest`.
- Guides: `docs/user-guide/f0-platform.md`, `docs/technical/f0-platform.md`.

### Changed
- **Swagger UI and api-docs are closed by default**: `SessionTokenSecurityConfig` only permits them when `umdc.docs.enabled=true` (default false; true under the `dev` profile and in tests); springdoc is switched off with the same property.
- **Coverage is measured for real**: JaCoCo `prepare-agent` runs in the default build and Surefire uses `@{jacoco.argLine}` (late replacement), so `mvn clean verify` writes `jacoco.exec` and `jacoco-check` no longer skips. The `coverage` profile was removed. `jacoco-check` now also requires METHOD coverage >= 85 % (measured 93.4 %).

### Notes
- In-memory buckets are per instance; with several replicas the effective quota multiplies until a Redis store exists.

## [Unreleased] - 2026-11-21 (locations and search by city)

### Added
- **Cities catalogue and business locations (sedes).** New `directory_site.city` and `directory_site.location` tables (migration `V4__create_city_and_location.sql`, applied by hand like `V2`/`V3`), seeded with Toronto and eight more Ontario cities. A location points to a business and to a city and carries an address, optional name, postal code, phone and coordinates, and a `main` flag (at most one main location per business, enforced by a partial unique index; the first location is always the main one). Indexes on `business_fk`, `city_fk` and `lower(city.name)`.
- `GET /api/v1/cities?country=&q=` lists the catalogue.
- `GET|POST /api/v1/businesses/{businessId}/locations` and `PATCH|DELETE /api/v1/businesses/{businessId}/locations/{locationId}`: any session can read; only the owner of the business (the `uid` of the `session-token`) can create, change or delete. Making a location main demotes the previous one; deleting the main one promotes the oldest remaining one; `main=false` on the main location is rejected (400).
- `GET /api/v1/businesses/search?city_id=|city=&category_id=&q=&page=&per_page=`: paginated search of businesses by city (id or name), category (sub-categories included) and text (name or description, `%` and `_` are literals), same envelope as the product listing, ordered by name then id, each result with its locations. Locations are loaded with one extra query per page.
- `BusinessRepository` now extends `JpaSpecificationExecutor` (`BusinessSpecifications`, `CitySpecifications`); `src/main/resources/api/index.yaml` documents the new endpoints.
- Guides: `docs/user-guide/locations-and-search.md`, `docs/technical/locations-and-search.md`.

### Tests
- `BusinessSearchRepositoryTest` (12, `@DataJpaTest` on H2 in PostgreSQL mode: real queries for the city `EXISTS`, the category/parent join, `LIKE ... ESCAPE`, the locations queries and the cities filter), `LocationServiceImplTest` (24), `CityServiceImplTest`, `BusinessSearchServiceImplTest` (3) and `LocationAndSearchRoutesTest` (8, URLs, parameters, and that `/businesses/search` is not taken for `/businesses/{id}`). New test dependency `spring-boot-starter-data-jpa-test` (`@DataJpaTest` lives there since Spring Boot 4).
- Found by the repository test: Spring Data JPA 4 rejects `null` specifications, so absent filters return `Specification.unrestricted()`.

### Notes
- **Action needed:** apply `V4` before deploying (and `V2`/`V3` if they were not applied yet). Not run against a real PostgreSQL here; H2 stands in for it in the tests.
- Search needs a `session-token` like the rest of `/api/v1/**`; the BFF supplies it.

---

## [Unreleased] - 2026-11-23 (F1-A geo search: nearby and map)

### Added
- `GET /api/v1/businesses/nearby` (public): locations within `radius_m` (default 5000, max 50000) of `lat`/`lng`, with `category_id` (sub-categories included), `city_id`, `q` (`%`, `_` escaped), `sort=distance|name`, `page`/`per_page` (1-based, max 100). One row per location with `distance_m`; ordered by distance with a stable tie-break by location id. Same envelope as the business search.
- `GET /api/v1/businesses/map` (public): GeoJSON `FeatureCollection` for `bbox=minLng,minLat,maxLng,maxLat` (inverted bbox or more than 90 degrees per axis is rejected), `zoom` 0-22, `category_id`, `city_id`, `q`, `limit` (default 200, max 500). Zoom 14 and above returns Point features; below 14 returns grid clusters (`cluster=true`, `count`). `truncated` is true when the limit was reached. Coordinates have 5 decimals.
- `LocationGeoRepository`: Haversine distance in portable SQL (PostgreSQL and H2 in PostgreSQL mode, no PostGIS) with a bounding-box pre-filter; only locations with coordinates.
- `V8__location_geo_index.sql`: partial index `idx_location_geo` on `(latitude, longitude)`. Not applied to any database yet.
- Invalid parameters answer `400` with the reason in the `message-error` header. Exact coordinates are never logged.
- Guides: `docs/user-guide/f1-geo-search.md`, `docs/technical/f1-geo-search.md`.

### Tests
- `LocationGeoRepositoryTest` (`@DataJpaTest` on H2: radius, order, tie-break, filters, pagination, bbox, clusters), `GeoSearchValidatorTest`, `BusinessGeoServiceImplTest`, `BusinessGeoRoutesTest` (the literal routes win over `/businesses/{id}`), `GeoToTest`.

### Notes
- `src/main/resources/api/index.yaml` is not updated in this change; the contract is in the technical guide.

## [Unreleased] - 2026-11-21 (database migration scripts)

### Added
- `src/main/resources/db/migration/V2__add_deleted_fields_user_favorite.sql`: adds nullable `deleted_at` / `deleted_by` to `directory_site.user_favorite` (used by `UserFavoriteEntity`).
- `src/main/resources/db/migration/V3__add_profile_image_ref_to_business.sql`: adds nullable `profile_image_ref VARCHAR(512)` to `directory_site.business` (used by `BusinessEntity` and the Cloudflare R2 image upload). It was announced in the 2026-04-12 entry but never committed.
- Both scripts are idempotent (`ADD COLUMN IF NOT EXISTS`). Guides: `docs/user-guide/database-migrations.md`, `docs/technical/database-migrations.md`.

### Notes
- Flyway is **not** configured in this project (no dependency, no properties), so the scripts are applied manually; no `V1` exists. No Java, test or runtime configuration changes.
## [Unreleased] - 2026-11-23 (F1-B location opening hours)

### Added
- Opening hours per location (RF-09): `GET` and `PUT /api/v1/businesses/{businessId}/locations/{locationId}/hours` (read is public; replacing the schedule is owner only, with 400/401/403/404 and the reason in the `message-error` header) and `GET .../hours/status?at=` returning `open_now`, `closes_at` or `opens_at`, and `timezone`.
- Pure `OpeningHoursEvaluator` (`isOpenNow`, `nextChange`) evaluated in the business time zone (default `America/Toronto`), with daylight saving and midnight handling.
- Migration script `V6__create_location_opening_hours.sql` (not applied by this change; Flyway stays off).
- Guides: `docs/user-guide/f1-opening-hours.md`, `docs/technical/f1-opening-hours.md`.

### Notes
- No existing location, search or configuration file was changed. Adding `open_now` to the search results is a follow-up (see the technical guide).

## [Unreleased] - 2026-11-21 (project documentation and agent tooling)

### Added
- Project documentation under `docs/`: `PROJECT_DOCUMENTATION.md`, `TECHNICAL_DOCUMENTATION.md`, `ARCHITECTURE_DIAGRAMS.md`, business-analyst set (`COMPLETE_PROJECT_DOCUMENTATION_FOR_BA.md`, `BA_QUICK_REFERENCE.md`, `BA_VISUAL_SUMMARY.md`), `DOCUMENTATION_INDEX.md`, `DELIVERY_SUMMARY.md`, `BUSINESS_PROFILE_IMAGE_API.md`, `esp8266.md`.
- AI-assistant tooling under `.github/`: agents (including the orchestrator), skills, prompts, hooks and tool descriptions.
- Task backlog under `issues/` and `youtrack/`, plus `variables_reference.md` and `ISSUE-business-patch-contacttype-transient.md`.
- `docs/user-guide/project-documentation.md` and `docs/technical/project-documentation.md`: where each document lives.

### Changed
- `BUILD_VALIDATION_REPORT.md` moved to `docs/BUILD_VALIDATION_REPORT.md`; `README.md` and the agent/skill/tool files refreshed.
- Documentation only: no source, test or configuration file changes.
## [Unreleased] - 2026-11-23 (F1-C server-side geocoding and pin validation)

### Added
- `POST /api/v1/geocode` (authenticated with `session-token`): body `{address, city?, country?}` (address required, max 200), answer `{candidates:[{label, latitude, longitude, confidence?}]}` with at most 5 in-range candidates. `401` without a valid token, `400` on invalid input, `502`/`504` on provider error/timeout, `503` while geocoding is disabled; the reason is in `message-error`.
- `GeocodingClient` port with a Feign implementation for a Maptiler-style provider. Disabled by default (`umdc.geocoding.enabled=false`); `umdc.geocoding.base-url`, `api-key` (environment variable or Vault only), `connect-timeout-ms`, `read-timeout-ms`, `max-candidates`. Results are never cached or stored, the address is never logged, and the API key never appears in logs or responses.
- `umdc.location.bounds.enforced` (default `true`).
- Tests with a mocked provider (no network): service, Feign client, controller, validator.
- Guides: `docs/user-guide/f1-geocoding.md`, `docs/technical/f1-geocoding.md`.

### Changed
- `LocationValidator` rejects the point (0, 0) and, while `umdc.location.bounds.enforced` is true, coordinates outside the Canada box (latitude 41.6 to 83.2, longitude -141.1 to -52.5), with `400`. `LocationServiceImplTest` used (1, 2) as an example pin and now uses (45, -75).

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

