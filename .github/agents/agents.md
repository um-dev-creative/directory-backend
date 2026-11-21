# AI Agents - Overview

This directory contains agent profiles used for delivery, implementation, review, and quality workflows in `directory-backend`.

## Agent Files and Assigned Skills

| Agent File | Agent Name | Invocable | Assigned Skills | Skill Definition |
|---|---|---|---|---|
| `orchestrator.agent.md` | Orchestrator | User | `release-management`, `ci-cd-orchestration`, `quality-gates`, `risk-assessment` | `skills/orchestrator/SKILL.md` |
| `api-reviewer.agent.md` | API Reviewer | Subagent | `openapi-specification`, `rest-api-design`, `api-contract-review` | `skills/api-reviewer/SKILL.md` |
| `code-reviewer.agent.md` | Code Reviewer | Subagent | `java-spring-development`, `java-code-quality`, `spring-boot-best-practices`, `clean-code`, `pmd-analysis` | `skills/code-reviewer/SKILL.md` |
| `database-architect.agent.md` | Database Architect | Subagent | `jpa-persistence`, `sql-optimization`, `schema-design`, `spring-data-jpa` | `skills/database-architect/SKILL.md` |
| `developer.agent.md` | Developer | User | `java-spring-development`, `rest-api-design`, `jpa-persistence`, `mapstruct-mapping`, `feign-integration`, `kafka-messaging`, `api-contract-review`, `junit5-testing` | `skills/developer/SKILL.md` |
| `devops-engineer.agent.md` | DevOps Engineer | Subagent | `release-management`, `ci-cd-orchestration`, `docker-containerization`, `github-actions`, `maven-build` | `skills/devops-engineer/SKILL.md` |
| `product-owner.agent.md` | Product Owner | User | `api-contract-review`, `acceptance-criteria`, `openapi-specification`, `backlog-management`, `rest-api-design` | `skills/product-owner/SKILL.md` |
| `project-manager.agent.md` | Project Manager | User | `release-management`, `risk-assessment`, `ci-cd-orchestration`, `quality-gates`, `sprint-planning`, `backlog-management`, `jacoco-coverage` | `skills/project-manager/SKILL.md` |
| `repo-requirements-analyst.agent.md` | Repo Requirements Analyst | User | `backlog-management`, `acceptance-criteria`, `risk-assessment` | `skills/repo-requirements-analyst/SKILL.md` |
| `security-reviewer.agent.md` | Security Reviewer | Subagent | `cve-detection`, `dependency-audit`, `owasp-top10`, `jwt-security`, `spring-security` | `skills/security-reviewer/SKILL.md` |
| `test-writer.agent.md` | QA / Test Writer | User | `junit5-testing`, `mockito-mocking`, `spring-boot-testing`, `jacoco-coverage`, `test-design-patterns` | `skills/test-writer/SKILL.md` |

## Collaboration Model

- Agents collaborate through issues, pull requests, and CI workflows.
- The **Orchestrator** agent decomposes multi-step requests and delegates to specialized agents via `run_subagent`.
- Agent outputs should include actionable artifacts (code, tests, docs, and review notes).
- Human reviewers remain responsible for approval and merge decisions.

## Shared Constraints

- Follow repository conventions in `AGENTS.md` and `CLAUDE.md`.
- Preserve backward compatibility for `/api/v1/*` unless a request explicitly allows breaking changes.
- Never commit secrets or modify certificate/keystore files unless explicitly requested.
