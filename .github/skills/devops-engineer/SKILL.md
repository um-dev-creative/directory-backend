---
name: DevOps Engineer Skills
description: Consolidated skill set for build, Docker, CI/CD in directory-backend
applies-to: [DevOps Engineer]
---

# DevOps Engineer — Skill Definition

## 1. Project-Specific Patterns

- Build artifact: `mvn -DskipTests package` → `target/directory-backend.jar`.
- Full CI check: `mvn -B -V -e clean verify` (includes PMD, JaCoCo 80% gate, Surefire).
- Docker: `Dockerfile` at project root — produces image tagged `directory-backend:<version>`.
- CI/CD pipelines: `.github/workflows/ci.yml` (verify), `build.yml` (Sonar), `qodana_code_quality.yml`.
- Release tag convention: `v<major>.<minor>.<patch>` (e.g., `v0.0.1`).
- Config server + Vault required at runtime — use `default.env` for local env vars.

## 2. Naming Conventions

- Release branches: `release/<version>`
- Feature branches: `feature/<ticket>-<short-description>`
- Fix branches: `fix/<ticket>-<short-description>`
- Docker image: `prx/directory-backend:<version>`

## 3. Error Handling

- Build failure → check `target/surefire-reports/` for test failures.
- Coverage failure → check `target/site/jacoco/index.html`.
- PMD failure → check `target/pmd.xml`.
- Docker build failure → validate `JAVA_HOME` and base image availability.

## 4. Key Files

- `Dockerfile`
- `.github/workflows/ci.yml`
- `.github/workflows/build.yml`
- `.github/workflows/qodana_code_quality.yml`
- `pom.xml` — versions, plugin config
- `default.env` — local env template (never commit secrets)
- `.github/tools/maven-build.tool.md`
- `.github/tools/docker-build.tool.md`
- `.github/tools/github-cli.tool.md`

## 5. Constraints

- Never commit secrets or tokens to any file.
- Do not modify certificate/keystore assets unless explicitly requested.
- All releases must pass `mvn -B -V -e clean verify` before tagging.

## 6. Checklist

- [ ] `mvn -B -V -e clean verify` passes
- [ ] Docker image builds successfully (`docker build -t prx/directory-backend:<version> .`)
- [ ] Release tag follows `v<major>.<minor>.<patch>` convention
- [ ] `CHANGELOG.md` updated
- [ ] CI workflows reference correct branch filters
- [ ] No secrets in pipeline YAML — all via GitHub Secrets

