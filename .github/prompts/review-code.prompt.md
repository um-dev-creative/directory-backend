---
name: Review Code
description: Review changed files for code quality, convention compliance, and correctness in directory-backend.
mode: agent
agent: code-reviewer
tools: [run_in_terminal, read_file, grep_search, file_search, get_errors]
---

# Review Code

## Input Variables

- `${changedFiles}` — list of changed files (e.g., from `git diff --name-only HEAD~1`) or PR diff
- `${prDescription}` — PR title and description

## Steps

1. **Read all changed files** in `${changedFiles}`.

2. **Check architecture conventions**:
   - Controllers (`*Controller.java`) are thin delegators — no business logic.
   - Services (`*ServiceImpl.java`) return `ResponseEntity` — not raw domain objects.
   - `*Api.java` holds Swagger annotations, not `*Controller.java`.
   - MapStruct mappers used for DTO ↔ Entity — no manual mapping in services.

3. **Check constant usage**:
   - No new hardcoded string literals — use `DirectoryAppConstants`.
   - `MESSAGE_HEADER` and `MESSAGE_ERROR_HEADER` set on all response entities.

4. **Run static analysis**:
   ```bash
   mvn pmd:check
   ```
   Assert: 0 violations.

5. **Check test coverage impact**:
   ```bash
   mvn clean test jacoco:report
   ```
   Assert: coverage ≥ 80%.

6. **Check API contract consistency** (if `*Api.java` or `index.yaml` changed):
   - Both files updated together.
   - No breaking changes without approval.

7. **Security spot-check**:
   - No secrets/tokens in changed files.
   - No stack traces exposed in response bodies.

## Constraints

- Do not suggest changes outside the scope of `${changedFiles}`.
- Report findings as actionable items with file:line references.

## Output Format

```markdown
## Code Review — ${prDescription}

### Convention Compliance
| Check | Status | Notes |
|-------|--------|-------|
| Controller is thin | ✅/❌ | |
| Service returns ResponseEntity | ✅/❌ | |
| MESSAGE_HEADER set | ✅/❌ | |
| DirectoryAppConstants used | ✅/❌ | |
| PMD 0 violations | ✅/❌ | |

### Findings
| Severity | File | Line | Issue | Suggestion |
|----------|------|------|-------|-----------|

### Quality Gates
| Gate | Result |
|------|--------|
| PMD | PASS/FAIL |
| Coverage | XX% (≥80%) |
| Tests | PASS/FAIL |

### Verdict
APPROVED / CHANGES REQUESTED
```

