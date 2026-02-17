# Changelog

All notable changes to this project will be documented in this file.
This project adheres (loosely) to "Keep a Changelog" and follows semantic versioning where practical.

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

