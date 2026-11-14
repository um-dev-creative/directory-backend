# Changelog

All notable changes to this project will be documented in this file.
This project adheres (loosely) to "Keep a Changelog" and follows semantic versioning where practical.

## [Unreleased] - 2026-11-13

### Changed
- **Breaking for operators.** Directory no longer logs in to mercury with an alias and a password. It asks backbone-rest for the Backbone M2M token of its own managed client (`POST /api/v1/managed-clients/token`, scopes `mercury:message:read` and `mercury:message:write`), sends it as `Authorization: Bearer` to mercury's `POST /api/v1/auth/token` and sends the M2M token mercury answers (tied to the Directory application) in `session-token`. New `MercuryTokens` caches both tokens until a minute before they expire and retries once with fresh tokens on a 401; `MercuryAuthClient` and `BackboneManagedClient` are Feign clients without the OAuth interceptor. `MercuryClient.token` and the `session-token-bkd` header towards mercury are gone, and `UserRegisterServiceImpl.confirmCode` no longer reads the user from backbone (it sent that user's alias and password to mercury). **Action needed, in this order:** (1) deploy backbone-rest, `security-oauth` and mercury with the Backbone-token exchange; (2) create Directory's managed client in Backbone, tied to the Directory application, with both scopes; (3) put `MERCURY_M2M_CLIENT_ID` and `MERCURY_M2M_CLIENT_SECRET` in Vault (optional `MERCURY_M2M_SCOPES`); (4) deploy this. See `docs/technical/mercury-m2m-auth.md` and `docs/user-guide/mercury-m2m-auth.md`.

### Tests
- Method coverage of the whole project went from 67.75 % (437/645) to 93.95 % (606/645). New: `TransferObjectsSmokeTest` (every record builds, prints and keeps `equals`/`hashCode` consistent), `MapperImplementationsTest` (every MapStruct method runs on fully populated arguments), `DefaultInterfaceMethodsTest` (every default method of the service/API interfaces answers 501 or throws `UnsupportedOperationException`), plus `SessionJwtServiceImplTest`, `KafkaProducerConfigTest` and `MercuryTokensTest`. The first three discover their classes from the classpath, so new records, mappers and interfaces are covered without editing them; their fixtures are in `src/test/java/com/prx/directory/support`. They are smoke tests: they prove nothing throws, not that a mapping is semantically right.

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

