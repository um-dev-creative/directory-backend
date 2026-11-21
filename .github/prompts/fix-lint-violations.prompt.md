---
name: Fix Lint Violations
description: Resolve PMD and code quality violations in directory-backend.
mode: agent
agent: code-reviewer
tools: [run_in_terminal, read_file, replace_string_in_file, grep_search, file_search, get_errors]
---

# Fix Lint Violations

## Input Variables

- `${violationReport}` — PMD report path or violation summary (e.g., `target/pmd.xml`, or pasted violation list)
- `${targetFiles}` — comma-separated list of files to fix (e.g., `CampaignServiceImpl.java,BusinessController.java`), or `all` for entire codebase

## Steps

1. **Generate current violation report**:
   ```bash
   mvn pmd:pmd
   grep -E '<violation|rule=' target/pmd.xml | head -60
   ```

2. **Parse violations** — for each violation, identify:
   - Rule name (e.g., `UnusedImports`, `NullAssignment`, `CyclomaticComplexity`)
   - File and line number
   - Severity

3. **Fix violations** in `${targetFiles}` (or all files if `all`):
   - Remove unused imports.
   - Replace string literals with `DirectoryAppConstants` equivalents.
   - Extract complex methods if `CyclomaticComplexity` violation.
   - Do not introduce new logic — fix lint only.

4. **Validate**:
   ```bash
   mvn pmd:check
   ```
   Assert: 0 violations.

5. **Verify no tests broken**:
   ```bash
   mvn test
   ```

## Constraints

- Do not refactor beyond what is needed to fix the violation.
- Do not reduce test coverage while fixing lint.
- Do not modify `*.crt`, `keystore.jks`, or `default.env`.

## Output Format

```markdown
## Lint Fix Report

### Violations Fixed
| File | Rule | Line | Fix Applied |
|------|------|------|-------------|

### Remaining Violations (if any)
| File | Rule | Reason Not Fixed |
|------|------|-----------------|

### Validation
`mvn pmd:check` → 0 violations
`mvn test` → all tests pass
```

