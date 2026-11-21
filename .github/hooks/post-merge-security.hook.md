---
name: Post Merge Security Check
description: Non-blocking security audit triggered after a merge to develop when pom.xml changed.
trigger: push.develop
agents: [security-reviewer]
auto-block: false
---

# Post Merge Security Check

## Trigger Conditions

- **Event:** Push to `develop` (merge)
- **File filter:** `pom.xml` changed

## Steps

### Step 1 — Dependency Security Audit

Agent: `security-reviewer`
Prompt: `.github/prompts/security-audit.prompt.md`
Input: `auditScope = deps-only`

Fail behavior:
- HIGH CVE found: open a GitHub issue labelled `security`, `priority:high`.
- MEDIUM CVE found: open a GitHub issue labelled `security`, `priority:medium`.
- LOW CVE: log only, no issue.

## Output

GitHub issue body when CVEs found:

```markdown
## Security Alert — Dependency CVE(s) Found after merge to develop

| Dependency | CVE | Severity | Current Version | Fix Version |
|------------|-----|----------|-----------------|-------------|

**Action:** Update affected dependencies in `pom.xml` before the next release.
**Assigned to:** security-reviewer, developer
```

