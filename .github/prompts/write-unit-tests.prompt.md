---
name: Write Unit Tests
description: Write JUnit 5 / Mockito unit and integration tests for directory-backend classes.
mode: agent
agent: test-writer
tools: [run_in_terminal, read_file, insert_edit_into_file, replace_string_in_file, create_file, grep_search, file_search, get_errors]
---

# Write Unit Tests

## Input Variables

- `${targetClass}` — fully qualified class to test (e.g., `com.umdc.service.v1.api.directory.CampaignServiceImpl`)
- `${testScenarios}` — comma-separated list of scenarios to cover (e.g., `listReturnsOk,listReturnsEmpty,createReturns201,createValidationFails`)

## Steps

1. **Read the target class** and identify:
   - Public methods and their signatures.
   - Dependencies (inject via constructor or `@Autowired`).
   - Return types (`ResponseEntity<T>`, etc.).

2. **Read existing test** (if any):
   - `src/test/java/.../${targetClass}Test.java`

3. **Create/update test class**:
   - Annotate: `@SpringBootTest` (integration) or `@ExtendWith(MockitoExtension.class)` (unit).
   - Add `@ActiveProfiles("test")` for Spring context tests.
   - Mock all external dependencies (`@MockBean` for Spring, `@Mock` for Mockito).
   - Cover each scenario in `${testScenarios}`:
     - Happy path → expected HTTP status + headers.
     - Error paths → `4xx` responses with `MESSAGE_ERROR_HEADER`.

4. **Assert response structure**:
   ```java
   // For controller tests via MockMvc
   mockMvc.perform(get("/api/v1/..."))
       .andExpect(status().isOk())
       .andExpect(header().exists(DirectoryAppConstants.MESSAGE_HEADER));
   ```

5. **Validate coverage**:
   ```bash
   mvn clean test jacoco:report
   # open target/site/jacoco/index.html
   ```
   Assert: line coverage ≥ 80%.

## Constraints

- Do not connect to real databases, Vault, Kafka, or Backbone/Mercury in unit tests.
- Do not use `@Disabled` without a `// TODO` explaining when to re-enable.
- Test method names must be descriptive: `<scenario>Returns<ExpectedResult>`.
- Use `application-test.yml` profile (H2 in-memory, security disabled).

## Output Format

```markdown
## Test Report — ${targetClass}

### Test Class
`src/test/java/.../${targetClass}Test.java`

### Scenarios Covered
| Method | Scenario | Assertion |
|--------|----------|-----------|

### Coverage After
- Line: XX%
- Branch: XX%

### Validation
`mvn test` → all N tests pass
```

