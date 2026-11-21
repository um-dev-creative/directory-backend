---
name: Project Manager Skills
description: Consolidated skill set for delivery coordination and release management in directory-backend
applies-to: [Project Manager]
---

# Project Manager — Skill Definition

## 1. Project-Specific Patterns

- Release tags: `v<major>.<minor>.<patch>` — created after `mvn -B -V -e clean verify` passes.
- `CHANGELOG.md` must be updated before every release tag.
- Quality gates: JaCoCo ≥ 80% line, PMD 0 violations, Sonar/Qodana green.
- CI pipelines: `ci.yml` (verify), `build.yml` (Sonar), `qodana_code_quality.yml` — monitor for failures.
- Sprint coordination: align with domain module delivery (Campaign, Business, Product, etc.).

## 2. Naming Conventions

- Release branches: `release/<version>`
- Milestones: `v<version>`
- GitHub issues: labelled with domain module and type (`bug`, `feature`, `security`).

## 3. Error Handling

- CI failure → escalate to `developer` or `devops-engineer` for diagnosis.
- Coverage drop below 80% → escalate to `test-writer`.
- Security CVE → escalate to `security-reviewer`.

## 4. Key Files

- `CHANGELOG.md`
- `.github/workflows/ci.yml`
- `.github/workflows/build.yml`
- `pom.xml` — version field `<version>`
- `.github/tools/github-cli.tool.md`
- `.github/tools/maven-build.tool.md`

## 5. Constraints

- Never create a release tag without passing CI.
- Never skip security review before a release if `pom.xml` changed.

## 6. Checklist

- [ ] All sprint tasks completed and verified
- [ ] `mvn -B -V -e clean verify` passes
- [ ] JaCoCo ≥ 80% confirmed
- [ ] PMD 0 violations confirmed
- [ ] `CHANGELOG.md` updated
- [ ] Release tag `v<version>` created
- [ ] GitHub release notes published via `gh release create`

