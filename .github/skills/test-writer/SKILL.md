---
name: Test Writer Skills
description: Consolidated skill set for JUnit 5 / Mockito test authoring in directory-backend
applies-to: [QA / Test Writer]
---

# QA / Test Writer — Skill Definition

## 1. Project-Specific Patterns

- Test profile: `application-test.yml` — H2 in-memory DB, Vault/Config Server/Kafka/Security disabled.
- Annotate test classes: `@SpringBootTest` or `@WebMvcTest` for controllers; `@ExtendWith(MockitoExtension.class)` for pure unit tests.
- Use `@ActiveProfiles("test")` to activate the test profile.
- Mock external Feign clients (`BackboneClient`, `MercuryClient`) in controller/service tests.
- JaCoCo gate: **80% line coverage** at bundle level — check with `mvn clean test jacoco:report`.
- JMH benchmark tests reside in `src/main/java/com/prx/directory/benchmark/` — do not include in unit test coverage.

## 2. Naming Conventions

| Artifact | Pattern | Example |
|----------|---------|---------|
| Test class | `*Test.java` | `CampaignControllerTest.java` |
| Test method (JUnit 5) | `<scenario><Expected>` | `listCampaignsReturnsOk()` |
| Mock field | `@Mock` / `@MockBean` | `@MockBean CampaignService campaignService` |
| Argument capture | `ArgumentCaptor<T>` | `ArgumentCaptor<CampaignRequest>` |

## 3. Error Handling (in tests)

- Assert HTTP status with `andExpect(status().isOk())` etc.
- Assert response headers: `andExpect(header().string(DirectoryAppConstants.MESSAGE_HEADER, ...))`.
- Use `assertThrows(...)` for exception path tests.

## 4. Key Files

- `src/test/java/com/prx/directory/` — all test sources
- `src/test/resources/application-test.yml` — test profile config
- `target/site/jacoco/index.html` — coverage report
- `target/surefire-reports/` — test reports

## 5. Constraints

- Never reduce existing coverage below 80%.
- Do not use `@Disabled` without a documented reason.
- Do not test private methods directly — test through public API.
- Do not connect to real databases, Vault, or Kafka in unit tests.

## 6. Checklist

- [ ] Test class annotated correctly (`@SpringBootTest`, `@WebMvcTest`, or `@ExtendWith`)
- [ ] `@ActiveProfiles("test")` applied
- [ ] All external dependencies mocked
- [ ] Happy path + at least one error path tested per method
- [ ] Assertions cover HTTP status AND response body/headers
- [ ] `mvn clean test jacoco:report` shows ≥ 80% line coverage
- [ ] No `@Disabled` tests without justification

