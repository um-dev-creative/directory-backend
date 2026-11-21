---
name: Full Feature Delivery
description: End-to-end orchestration of a new feature across all agents — implementation, tests, review, and API contract validation.
mode: agent
agent: orchestrator
tools: [run_in_terminal, read_file, grep_search, file_search, run_subagent]
---

# Full Feature Delivery

## Input Variables

- `${storyDescription}` — the user story or feature description (e.g., "Add status filter to Campaign list endpoint")
- `${domainModule}` — affected domain module (User | Business | Campaign | Category | Product | DigitalContact | Favorite | Timezone | Auth)
- `${apiPath}` — target REST path (e.g., `/api/v1/campaign`)
- `${httpMethod}` — HTTP method if applicable (GET | POST | PUT | PATCH | DELETE)

## Steps

### Step 1 — Requirements (Product Owner)
Delegate to `product-owner`:
> "Define acceptance criteria for: ${storyDescription}. Domain: ${domainModule}. API: ${httpMethod} ${apiPath}."

### Step 2 — Implementation (Developer)
Delegate to `developer` using `implement-feature.prompt.md`:
> "Implement: ${storyDescription}. Domain: ${domainModule}. Path: ${apiPath}. Method: ${httpMethod}. Follow all conventions in AGENTS.md and developer SKILL.md."

### Step 3 — Tests (Test Writer) — after Step 2
Delegate to `test-writer` using `write-unit-tests.prompt.md`:
> "Write unit and integration tests for the ${domainModule} feature implemented in Step 2. Target class: ${domainModule}Controller, ${domainModule}ServiceImpl. Ensure JaCoCo ≥ 80%."

### Step 4 — Code Review (Code Reviewer) — after Step 3
Delegate to `code-reviewer` using `review-code.prompt.md`:
> "Review the ${domainModule} implementation and tests. Check PMD, convention compliance, ResponseEntity usage, MESSAGE_HEADER, DirectoryAppConstants."

### Step 5 — API Contract Review (API Reviewer) — parallel with Step 4
If `${apiPath}` is new or modified, delegate to `api-reviewer` using `review-api-contract.prompt.md`:
> "Review API contract changes for ${httpMethod} ${apiPath}. Validate *Api.java annotations match src/main/resources/api/index.yaml."

### Step 6 — Validate Build
```bash
mvn -B -V -e clean verify
```
Assert: BUILD SUCCESS, JaCoCo ≥ 80%, PMD 0 violations.

## Constraints

- Do not proceed to Step 3 until Step 2 produces compiling code.
- Do not proceed to Step 6 until Steps 3–5 complete.
- Never break `/api/v1/*` backward compatibility without explicit user approval.
- Never commit secrets or modify certificate/keystore files.

## Output Format

```markdown
## Delivery Summary — ${storyDescription}

### Tasks Completed
| Task | Agent | Status | Artifacts |
|------|-------|--------|-----------|
| Requirements | product-owner | ✅ | acceptance-criteria.md |
| Implementation | developer | ✅ | list of changed files |
| Tests | test-writer | ✅ | list of test files |
| Code Review | code-reviewer | ✅ | review findings |
| API Contract | api-reviewer | ✅ | contract validation result |

### Quality Gates
| Gate | Result |
|------|--------|
| Build (`mvn verify`) | PASS |
| Tests | PASS (N/N) |
| Coverage | XX% (≥80%) |
| PMD | PASS (0 violations) |

### Files Changed
- list

### Follow-up Actions
- list
```

