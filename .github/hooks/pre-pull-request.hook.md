---
name: Pre Pull Request Gate
description: Blocking quality gate triggered when a PR is opened to main or develop. Runs code review, API contract validation, and security audit.
trigger: pull_request.opened
agents: [code-reviewer, api-reviewer, security-reviewer]
auto-block: true
---

# Pre Pull Request Gate

## Trigger Conditions

- **Event:** Pull request opened or reopened
- **Target branches:** `main`, `develop`
- **File filter:** any `src/` change

## Steps

### Step 1 — Code Review (always runs)
Agent: `code-reviewer`
Prompt: `.github/prompts/review-code.prompt.md`

```
changedFiles = git diff --name-only origin/main...HEAD
prDescription = PR title + description
```

Fail behavior: **block merge** — post review findings as PR comment.

### Step 2 — API Contract Review (conditional)
Agent: `api-reviewer`
Prompt: `.github/prompts/review-api-contract.prompt.md`

**Condition:** runs only if changed files include `*Api.java` or `index.yaml`.

```
changedApiFiles = filtered list from Step 1
openApiSpecPath = src/main/resources/api/index.yaml
```

Fail behavior: **block merge** — post contract violations as PR comment.

### Step 3 — Security Audit (conditional)
Agent: `security-reviewer`
Prompt: `.github/prompts/security-audit.prompt.md`

**Condition:** runs only if changed files include `pom.xml`.

```
auditScope = deps-only
```

Fail behavior: **block merge** if HIGH severity CVE found — post security report as PR comment.

## Fail Behavior Summary

| Step | Failure | Action |
|------|---------|--------|
| Code Review | Violations found | Block merge + comment |
| API Contract | Breaking change without approval | Block merge + comment |
| Security Audit | HIGH CVE | Block merge + comment |
| Security Audit | MEDIUM CVE | Warn + comment (does not block) |

## Output

Post a structured comment on the PR:

```markdown
## 🤖 Pre-PR Quality Gate

### Code Review
Status: PASS / FAIL
Findings: N items

### API Contract (if applicable)
Status: PASS / FAIL / NOT APPLICABLE

### Security (if applicable)
Status: PASS / WARN / FAIL / NOT APPLICABLE

### Overall: ✅ APPROVED TO MERGE / ❌ CHANGES REQUIRED
```

