# Hooks Catalog

This folder defines event-driven agent triggers for `directory-backend`. Hooks specify when agents are invoked automatically based on development lifecycle events.

## All Hooks

| Hook | File | Trigger | Blocking | Agents |
|------|------|---------|---------|--------|
| Pre Pull Request Gate | `pre-pull-request.hook.md` | PR opened to `main`/`develop` | **Yes** | code-reviewer, api-reviewer, security-reviewer |
| Post Implementation Review | `post-implementation-review.hook.md` | Push to `feature/*` or `fix/*` | No | test-writer, code-reviewer |
| Post Merge Security Check | `post-merge-security.hook.md` | Merge to `develop` (`pom.xml` changed) | No | security-reviewer |
| Pre Release Gate | `pre-release-gate.hook.md` | Before tag `v*` | **Yes** | security-reviewer, api-reviewer, code-reviewer, devops-engineer |

## Lifecycle Diagram

```
Developer pushes to feature/* or fix/*
        │
        ▼
[post-implementation-review] ──► advisory comment (non-blocking)
        │
        ▼
Developer opens PR to develop / main
        │
        ▼
[pre-pull-request] ──────────────► BLOCKING gate
    ├── code-reviewer (always)
    ├── api-reviewer (if *Api.java or index.yaml changed)
    └── security-reviewer (if pom.xml changed)
        │ PASS
        ▼
PR merged to develop
        │
        ├── pom.xml changed?
        │       │
        │       ▼
        │   [post-merge-security] ──► GitHub issue if CVE found (non-blocking)
        │
        ▼
Release preparation begins
        │
        ▼
[pre-release-gate] ──────────────► BLOCKING gate
    ├── security-reviewer (full audit)
    ├── api-reviewer (contract check)
    ├── code-reviewer (full diff since last tag)
    └── devops-engineer (prepare-release) ──► tag + publish
```

## Integration Notes

- Hook definitions here are **documentation conventions** — they describe agent-automation intent.
- Corresponding GitHub Actions automation is in `.github/workflows/ci.yml`, `build.yml`, `qodana_code_quality.yml`.
- To add automation for a hook, create a new workflow in `.github/workflows/` that calls the relevant agent prompt steps.
- All blocking hooks must have `auto-block: true` — their failure must prevent merge/tag via branch protection rules.

## Prompt References

| Hook | Prompts Used |
|------|-------------|
| pre-pull-request | `review-code.prompt.md`, `review-api-contract.prompt.md`, `security-audit.prompt.md` |
| post-implementation-review | `fix-lint-violations.prompt.md`, `write-unit-tests.prompt.md` |
| post-merge-security | `security-audit.prompt.md` |
| pre-release-gate | `security-audit.prompt.md`, `review-api-contract.prompt.md`, `review-code.prompt.md`, `prepare-release.prompt.md` |

