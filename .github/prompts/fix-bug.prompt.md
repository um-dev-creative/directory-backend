---
name: Fix Bug
description: Diagnose and fix a bug in directory-backend following project conventions.
mode: agent
agent: developer
tools: [run_in_terminal, read_file, insert_edit_into_file, replace_string_in_file, grep_search, file_search, get_errors]
---

# Fix Bug

## Input Variables

- `${bugDescription}` — description of the bug (e.g., "NullPointerException in BusinessServiceImpl.update when contactType is null")
- `${affectedClass}` — primary class or file suspected (e.g., `BusinessServiceImpl`)
- `${reproduceSteps}` — steps to reproduce or test case name (e.g., `mvn -Dtest=BusinessServiceImplTest#updateReturnsNotFound test`)

## Steps

1. **Reproduce the bug**:
   ```bash
   ${reproduceSteps}
   ```

2. **Read affected code**:
   - `src/main/java/com/prx/directory/**/${affectedClass}.java`
   - Related service, controller, and entity files.

3. **Identify root cause** — trace the call chain: Controller → Service → Repository.

4. **Apply minimal fix**:
   - Preserve existing behavior for all other paths.
   - Use `DirectoryAppConstants` for any new string literals.
   - Set correct HTTP status and `MESSAGE_HEADER` on affected responses.

5. **Validate fix**:
   ```bash
   mvn test
   ```
   Assert: failing test now passes; no other tests broken.

## Constraints

- Fix only the reported bug — do not refactor unrelated code.
- Do not reduce test coverage — add a regression test if one doesn't exist.
- Do not modify `*.crt`, `keystore.jks`, or `default.env`.

## Output Format

```markdown
## Bug Fix — ${bugDescription}

### Root Cause
Description of root cause with file:line reference.

### Fix Applied
| File | Change |
|------|--------|
| path/to/file | description |

### Regression Test
- Test class: `XxxTest`
- Test method: `xxxReproduceBugFixed()`

### Validation
`mvn test` → all tests pass
```

