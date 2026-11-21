# Tools Catalog

This folder describes tools that AI agents can invoke or orchestrate when assisting with development tasks in `directory-backend`.

## Canonical Tool Files

| Tool | File | Type | Used By |
|------|------|------|---------|
| Maven Build | `maven-build.tool.md` | Terminal | Developer, DevOps Engineer, QA / Test Writer |
| Docker Build | `docker-build.tool.md` | Terminal | DevOps Engineer |
| GitHub CLI | `github-cli.tool.md` | Terminal | DevOps Engineer, Project Manager, Orchestrator |
| Dependency Check | `dependency-check.tool.md` | Terminal | Security Reviewer |
| OpenAPI Validator | `openapi-validator.tool.md` | Terminal | API Reviewer, Developer |
| Sonar Analysis | `sonar-analysis.tool.md` | Terminal | Code Reviewer, DevOps Engineer |
| Git | `git.tool.md` | Terminal | Developer, Orchestrator, DevOps Engineer |
| PMD and Qodana | `pmd-qodana.tool.md` | Terminal | Code Reviewer, Developer |

> **`maven.md` is deprecated** — redirects to `maven-build.tool.md`.

## Key Output Locations

| Tool | Report Location |
|------|----------------|
| Maven Surefire | `target/surefire-reports/` |
| JaCoCo coverage | `target/site/jacoco/index.html` |
| PMD | `target/pmd.xml` |
| CPD | `target/cpd.xml` |
| Dependency Check | `target/dependency-check-report.html` |
| Qodana | `qodana.sarif.json` |
| Sonar | SonarCloud dashboard |
