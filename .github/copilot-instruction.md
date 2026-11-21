# copilot-instruction.md

## Scope and Goal
- Repository: `directory-backend` (Spring Boot microservice, Java 21, Spring Cloud 2025.0.1).
- Goal: implement minimal, safe, backward-compatible changes for `/api/v1/*` behavior unless explicitly requested otherwise.

## Architecture and Request Flow
- Entry point: `src/main/java/com/prx/directory/DirectoryBackendApplication.java`.
- Package scan/enabled clients include `com.umdc.commons.services`, `com.umdc.directory`, `com.umdc.security`, and Feign clients.
- Standard flow: **`*Api.java` interface -> `*Controller.java` -> `*ServiceImpl.java` -> `*Repository.java`**.
- API interfaces (`*Api.java`) contain Swagger/OpenAPI annotations and default methods; controllers should remain thin.
- Service layer owns validation, status mapping, and typically returns `ResponseEntity<?>`.

## Naming and Structure Conventions
- DTOs: `src/main/java/com/prx/directory/api/v1/to`.
- Entities: `src/main/java/com/prx/directory/jpa/entity`.
- Repositories: `src/main/java/com/prx/directory/jpa/repository`.
- JPA specs/filters: `src/main/java/com/prx/directory/jpa/spec`.
- Mappers: `src/main/java/com/prx/directory/mapper/*Mapper.java` (MapStruct + `MapperAppConfig`).
- Prefer constants in `src/main/java/com/prx/directory/constant` (especially `DirectoryAppConstants`) over new string literals.

## API Compatibility and Contract Rules
- Preserve backward compatibility for `/api/v1/*` contracts by default.
- If API contract changes are required, update both:
  - Java API annotations in `*Api.java`
  - OpenAPI file: `src/main/resources/api/index.yaml`
- Preserve existing response/header semantics, including `DirectoryAppConstants.MESSAGE_HEADER` (and related error/header patterns).
- Keep legacy campaign filter aliases working (`q`, `category_id`, `business_id`, `start_date_*`, `end_date_*`).
- Keep `CampaignServiceImpl.update(...)` optimistic-lock behavior and `202 Accepted` semantics unless explicitly changed.

## Coding Standards
- Use clean, idiomatic Java 21 and Spring Boot 3 patterns.
- Keep edits small, atomic, and scoped; avoid unrelated refactors.
- Maintain existing package boundaries and layering.
- Add logging at appropriate levels (`trace`, `debug`, `info`, `warn`) where behavior is non-trivial.
- Favor explicit validation and clear error responses over silent fallback behavior.
- Keep mapping concerns in MapStruct mappers, not controllers.

## Integrations (Do Not Break)
- Backbone: `client/backbone/BackboneClient.java` (+ Feign configurer).
- Mercury: `client/mercury/MercuryClient.java` (+ Feign configurer).
- Auth/JWT flow: `services/AuthServiceImpl.java`, `security/SessionJwtServiceImpl.java`, `util/JwtUtil`.
- Kafka outbound email flow: `kafka/producer/EmailMessageProducerServiceImpl.java`.

## Build and Test Commands
```bash
# Fast package (skip tests)
mvn -DskipTests package

# CI-aligned full verification
mvn -B -V -e clean verify

# Run all tests
mvn test

# Run one test class
mvn -Dtest=com.umdc.controller.v1.api.directory.CampaignControllerTest test

# Run one test method
mvn -Dtest=CampaignControllerTest#listCampaignsReturnsOk test

# Coverage report
mvn clean test jacoco:report
```

## Quality Gates
- Keep JaCoCo line coverage at or above the repository threshold (80% bundle line coverage).
- Do not introduce changes that break existing tests without explicit approval.
- Add/adjust unit/integration tests for behavior changes.

## Safety and Security Constraints
- **Do not modify** certificate/keystore assets unless explicitly requested:
  - `src/main/resources/*.crt`
  - `keystore.jks`
- Treat `default.env` and environment/config data as sensitive:
  - never commit secrets, tokens, or credentials
  - do not print secrets in logs/tests
- Do not revert unrelated user changes in a dirty worktree.
- Avoid broad formatting/refactor churn; change only what is needed.

## Change Execution Checklist
- Confirm affected layer(s): API, service, repository, mapper, integration.
- Preserve `/api/v1/*` compatibility and legacy filter behavior.
- If contract changed: update both `*Api.java` and `src/main/resources/api/index.yaml`.
- Add/update tests and run relevant Maven commands.
- Keep security/safety constraints intact (no cert/env secret changes).

