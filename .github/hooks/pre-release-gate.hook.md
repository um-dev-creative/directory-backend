---
name: Pre Release Gate
description: Blocking gate executed before a release tag is created. Runs full security, API contract, code review, and release preparation.
trigger: pre-tag.v_wildcard
agents: [security-reviewer, api-reviewer, code-reviewer, devops-engineer]
auto-block: true
---

# Pre Release Gate

## Trigger Conditions

- **Event:** Before creating a tag matching `v*`
- **Branch filter:** `main`, `release/*`

## Steps

### Step 1 — Full Security Audit (blocking)

Agent: `security-reviewer`
Prompt: `.github/prompts/security-audit.prompt.md`
Input: `auditScope = full`

Fail: **block release** if HIGH severity CVE found.

### Step 2 — API Contract Review (blocking)

Agent: `api-reviewer`
Prompt: `.github/prompts/review-api-contract.prompt.md`
Input: `changedApiFiles = all *Api.java files and index.yaml`

Fail: **block release** if breaking change found without explicit user approval.

### Step 3 — Code Review (blocking)

Agent: `code-reviewer`
Prompt: `.github/prompts/review-code.prompt.md`
Input: `changedFiles = all files changed since last release tag`

Fail: **block release** if CRITICAL findings remain unresolved.

### Step 4 — Release Preparation (runs only if Steps 1-3 pass)

Agent: `devops-engineer`
Prompt: `.github/prompts/prepare-release.prompt.md`
Input: `releaseVersion = <version from tag>`, `releaseNotes = <from CHANGELOG.md>`

## Fail Behavior Summary

| Step | Failure | Action |
|------|---------|--------|
| Security Audit | HIGH CVE | Block + post gate report |
| API Contract | Breaking change | Block + require user approval |
| Code Review | Critical finding | Block + post gate report |
| Release Preparation | `mvn verify` failure | Block + post build log |

## Output

Gate report posted as a PR/commit comment or release check:

```markdown
## Release Gate Report — v<version>

| Gate | Status | Notes |
|------|--------|-------|
| Security (full) | PASS / FAIL | N CVEs found |
| API Contract | PASS / FAIL | Breaking: yes/no |
| Code Review | PASS / FAIL | N critical findings |
| Build (mvn verify) | PASS / FAIL | |
| JaCoCo | XX% (>=80%) | |
| PMD | 0 violations | |

### Verdict: RELEASE APPROVED / BLOCKED — reason
```

