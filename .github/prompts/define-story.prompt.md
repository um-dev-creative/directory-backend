---
name: Define Story
description: Translate a feature request into a structured user story with acceptance criteria for directory-backend.
mode: ask
agent: product-owner
tools: [read_file, grep_search, file_search]
---

# Define Story

## Input Variables

- `${featureRequest}` — raw feature request or idea (e.g., "Allow users to filter campaigns by status")

## Steps

1. **Clarify domain context** — read `src/main/resources/api/index.yaml` to understand existing API surface.

2. **Identify domain module** — which of: User, Business, Campaign, Category, Product, DigitalContact, Favorite, Timezone, Auth.

3. **Draft user story** in As/Want/So-That format.

4. **Define acceptance criteria** in Given/When/Then format — one criterion per observable behavior.

5. **Flag constraints**:
   - Breaking change to `/api/v1/*`? → requires explicit approval.
   - New entity or schema change? → assign `database-architect`.
   - New Feign integration? → assign `developer` with `feign-integration` skill.

## Output Format

```markdown
## Story: ${featureRequest}

### User Story
As a **<role>**,
I want **<feature>**,
so that **<benefit>**.

### Domain Module
<module name>

### API Impact
- Endpoint: `<METHOD> /api/v1/<path>`
- Change type: New | Modified | None
- Breaking change: Yes (requires approval) | No

### Acceptance Criteria

**AC-1: <scenario title>**
- Given: ...
- When: ...
- Then: ...

**AC-2: <scenario title>**
- Given: ...
- When: ...
- Then: ...

### Constraints / Notes
- list

### Agent Assignments
| Task | Agent |
|------|-------|
| Implementation | developer |
| Tests | test-writer |
| API review | api-reviewer |
| Schema (if needed) | database-architect |
```

