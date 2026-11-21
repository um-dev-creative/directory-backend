---
name: Review API Contract
description: Validate that *Api.java annotations and src/main/resources/api/index.yaml are consistent and backward-compatible.
mode: agent
agent: api-reviewer
tools: [run_in_terminal, read_file, grep_search, file_search, get_errors]
---

# Review API Contract

## Input Variables

- `${changedApiFiles}` — list of changed files related to API (e.g., `CampaignApi.java,index.yaml`)
- `${openApiSpecPath}` — path to OpenAPI spec (default: `src/main/resources/api/index.yaml`)

## Steps

1. **Read changed API files** listed in `${changedApiFiles}`.

2. **Read the OpenAPI spec** at `${openApiSpecPath}`.

3. **Validate OpenAPI spec syntax**:
   ```bash
   mvn springdoc-openapi:generate -q 2>&1 | tail -20
   ```

4. **Cross-check `*Api.java` ↔ `index.yaml`**:
   - Every `@Operation`-annotated method in `*Api.java` must have a corresponding path+method in `index.yaml`.
   - Request/response schemas must match DTOs in `com.umdc.directory.api.v1.to`.
   - HTTP status codes must be consistent.

5. **Backward-compatibility check**:
   - No existing required request fields removed or renamed.
   - No existing response fields removed or renamed.
   - No path or HTTP method removed.
   - If breaking change found: flag with severity HIGH and require explicit approval.

6. **Header documentation check**:
   - `MESSAGE_HEADER` documented in response headers for all mutating endpoints.

## Constraints

- Never approve a breaking change to `/api/v1/*` without explicit user confirmation.
- Both `*Api.java` and `index.yaml` must be updated together — flag if only one is changed.

## Output Format

```markdown
## API Contract Review

### Files Reviewed
- list

### Syntax Validation
`mvn springdoc-openapi:generate` → PASS/FAIL

### Cross-Check Results
| Endpoint | *Api.java | index.yaml | Schema Match | Status |
|----------|-----------|------------|--------------|--------|

### Backward Compatibility
| Field/Path | Type | Risk | Action Required |
|------------|------|------|----------------|

### Header Documentation
| Endpoint | MESSAGE_HEADER Documented |
|----------|--------------------------|

### Verdict
APPROVED / CHANGES REQUIRED / BREAKING CHANGE (requires user approval)
```

