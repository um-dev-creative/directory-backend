---
name: Implement Feature
description: Implement a new feature or enhancement following directory-backend conventions.
mode: agent
agent: developer
tools: [run_in_terminal, read_file, insert_edit_into_file, replace_string_in_file, create_file, grep_search, file_search, get_errors]
---

# Implement Feature

## Input Variables

- `${featureDescription}` — what to implement (e.g., "Add `status` filter to Campaign list endpoint")
- `${domainModule}` — domain module (User | Business | Campaign | Category | Product | DigitalContact | Favorite | Timezone | Auth)
- `${httpMethod}` — HTTP method (GET | POST | PUT | PATCH | DELETE)
- `${apiPath}` — REST path (e.g., `/api/v1/campaign`)

## Steps

1. **Read existing code** for the affected domain:
   - `src/main/java/com/prx/directory/api/v1/controller/${domainModule}Api.java`
   - `src/main/java/com/prx/directory/api/v1/controller/${domainModule}Controller.java`
   - `src/main/java/com/prx/directory/api/v1/service/${domainModule}ServiceImpl.java`
   - Related DTOs in `src/main/java/com/prx/directory/api/v1/to/`
   - Related entity in `src/main/java/com/prx/directory/jpa/entity/`

2. **Implement the feature** following project conventions:
   - API annotations in `*Api.java` only — controller is a thin delegator.
   - Service returns `ResponseEntity` directly — not raw domain objects.
   - Use `DirectoryAppConstants` for all string literals (no new hardcoded strings).
   - Set `MESSAGE_HEADER` on all response entities.
   - Use MapStruct mapper for DTO ↔ Entity conversion.
   - For update operations: check `lastUpdate` for optimistic concurrency → return `202 Accepted`.

3. **Update OpenAPI spec** `src/main/resources/api/index.yaml` to reflect any contract change.

4. **Validate compilation**:
   ```bash
   mvn -DskipTests package
   ```

## Constraints

- Preserve backward compatibility for `/api/v1/*` unless explicitly approved to break it.
- Do not modify `*.crt`, `keystore.jks`, or `default.env`.
- Do not refactor unrelated files — minimal, atomic changes only.
- Do not add new string literals — use or extend `DirectoryAppConstants`.

## Output Format

```markdown
## Implementation — ${featureDescription}

### Files Changed
| File | Change |
|------|--------|
| path/to/file | description |

### API Contract Changes
- New/modified: ${httpMethod} ${apiPath}
- index.yaml updated: yes/no

### Build Status
`mvn -DskipTests package` → SUCCESS / FAILURE + reason
```

