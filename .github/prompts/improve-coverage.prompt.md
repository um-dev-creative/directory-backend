---
name: Improve Coverage
description: Identify and fill test coverage gaps to reach or maintain the 80% JaCoCo line coverage gate.
mode: agent
agent: test-writer
tools: [run_in_terminal, read_file, insert_edit_into_file, replace_string_in_file, create_file, grep_search, file_search, get_errors]
---

# Improve Coverage

## Input Variables

- `${coverageReport}` — path to JaCoCo report or package/class with low coverage (e.g., `target/site/jacoco/index.html` or `com.umdc.directory.api.v1.service`)
- `${targetThreshold}` — minimum line coverage target (default: `80`)

## Steps

1. **Generate current coverage report**:
   ```bash
   mvn clean test jacoco:report
   ```

2. **Identify low-coverage classes**:
   ```bash
   # Classes below threshold in the report
   grep -r 'ctr2' target/site/jacoco/ | grep -v '100%' | head -30
   ```
   Focus on classes in:
   - `com.umdc.directory.api.v1.service.*`
   - `com.umdc.directory.api.v1.controller.*`
   - `com.umdc.directory.mapper.*`
   - `com.umdc.directory.jpa.spec.*`

3. **For each under-covered class**, identify uncovered branches:
   - Read the class source.
   - Check which methods/branches lack assertions.

4. **Write tests** for missing scenarios following `write-unit-tests.prompt.md` patterns:
   - Error paths (validation failures, not-found, conflict).
   - Edge cases (null inputs, empty collections, pagination boundaries).

5. **Re-validate**:
   ```bash
   mvn clean test jacoco:report
   ```
   Assert: bundle line coverage ≥ `${targetThreshold}`%.

## Constraints

- Do not write trivial tests (e.g., testing getters/setters) just to inflate coverage.
- Do not use `@Disabled` tests.
- Do not connect to real external services.

## Output Format

```markdown
## Coverage Improvement Report

### Before
- Bundle line coverage: XX%

### Classes Targeted
| Class | Before | Tests Added |
|-------|--------|-------------|

### After
- Bundle line coverage: XX% (≥ ${targetThreshold}%)

### Validation
`mvn -B -e clean verify` → BUILD SUCCESS
```

