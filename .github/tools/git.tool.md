---
name: Git
description: Git version control operations for directory-backend
type: terminal
command-prefix: git
used-by: [Developer, Orchestrator, DevOps Engineer]
---

# Git Tool

## Purpose

Manages source code versioning, branching, tagging, and history for `directory-backend`. Enforces branch naming and commit conventions.

## Branch Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feature/<ticket>-<description>` | `feature/DIR-42-campaign-filter` |
| Bug fix | `fix/<ticket>-<description>` | `fix/DIR-99-null-pointer-business` |
| Release | `release/<version>` | `release/0.2.0` |
| Hotfix | `hotfix/<version>-<description>` | `hotfix/0.1.1-jwt-expiry` |

## Available Commands

### Branch Management
```bash
# Create and switch to a new feature branch
git checkout -b feature/DIR-XX-short-description

# Push branch upstream
git push -u origin feature/DIR-XX-short-description

# List all branches
git branch -a
```

### Commit
```bash
# Stage and commit (conventional commit format)
git add src/
git commit -m "feat(campaign): add status filter to list endpoint"

# Conventional commit types: feat, fix, docs, style, refactor, test, chore
```

### Tags (Release)
```bash
# Create annotated release tag
git tag -a v0.2.0 -m "Release v0.2.0 — campaign filter improvements"

# Push tag
git push origin v0.2.0

# List tags
git tag --list 'v*'
```

### History and Diff
```bash
# Compact log
git --no-pager log --oneline -20

# Diff staged changes
git diff --staged

# Show changed files in last commit
git --no-pager show --stat HEAD
```

## Output Locations

- Branch list: stdout
- Tags: `git tag --list`
- Log: stdout

## Notes

- Never commit `default.env` or any file containing secrets/tokens.
- Never commit `src/main/resources/*.crt` or `keystore.jks` changes unless explicitly requested.
- Keep commits atomic — one logical change per commit.
- Use `git --no-pager` when running in non-interactive mode to avoid pager hangs.

