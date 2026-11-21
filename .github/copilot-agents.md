# AI Agent Coworkers — directory-backend

This document indexes all AI agents, skills, tools, prompts, and hooks configured
for the **directory-backend** Spring Boot microservice.

## Project Snapshot

| Field | Value |
|-------|-------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.8 · Spring Cloud 2025.0.1 |
| Build tool | Maven 3.x (`mvnw`) |
| Test framework | JUnit 5.14.1 · Mockito 5.21.0 · H2 (test profile) · JMH 1.37 |
| Static analysis | PMD 3.28.0 (`ruleset.xml`) · JaCoCo 0.8.14 (80% gate) · SonarCloud · Qodana |
| Config system | Spring Cloud Config + HashiCorp Vault |
| Entry point | `com.umdc.directory.DirectoryBackendApplication` |
| Domain modules | User, Business, Campaign, Category, Product, DigitalContact, Favorite, Timezone, Auth |

## Agents

### Primary Agents (User-Invocable)

| Agent | File | Description |
|-------|------|-------------|
| Orchestrator | `agents/orchestrator.agent.md` | Decomposes requests, delegates to subagents, produces delivery summaries |
| Developer | `agents/developer.agent.md` | Senior full-stack developer (Java/Spring) |
| QA / Test Writer | `agents/test-writer.agent.md` | Automated test authoring |
| Product Owner | `agents/product-owner.agent.md` | Business stakeholder / requirements |
| Project Manager | `agents/project-manager.agent.md` | Delivery lead / release coordination |
| Repo Requirements Analyst | `agents/repo-requirements-analyst.agent.md` | Codebase analysis / requirement discovery |

### Subagents (Agent-Invocable Only)

| Agent | File | Description |
|-------|------|-------------|
| API Reviewer | `agents/api-reviewer.agent.md` | OpenAPI contract validation |
| Security Reviewer | `agents/security-reviewer.agent.md` | CVE scanning / OWASP review |
| Code Reviewer | `agents/code-reviewer.agent.md` | Code quality / convention compliance |
| Database Architect | `agents/database-architect.agent.md` | JPA entity design / query optimization |
| DevOps Engineer | `agents/devops-engineer.agent.md` | CI/CD pipeline / Docker management |

See [`agents/agents.md`](agents/agents.md) for the full table with skill definitions.

## Skills

### Per-Agent Skill Definitions

| Agent | File |
|-------|------|
| Orchestrator | `skills/orchestrator/SKILL.md` |
| Developer | `skills/developer/SKILL.md` |
| QA / Test Writer | `skills/test-writer/SKILL.md` |
| Code Reviewer | `skills/code-reviewer/SKILL.md` |
| Security Reviewer | `skills/security-reviewer/SKILL.md` |
| DevOps Engineer | `skills/devops-engineer/SKILL.md` |
| API Reviewer | `skills/api-reviewer/SKILL.md` |
| Database Architect | `skills/database-architect/SKILL.md` |
| Product Owner | `skills/product-owner/SKILL.md` |
| Project Manager | `skills/project-manager/SKILL.md` |
| Repo Requirements Analyst | `skills/repo-requirements-analyst/SKILL.md` |

See [`skills/skills.md`](skills/skills.md) for the full shared skill library (35 shared skills).

## Tools

| Tool | File | Used By |
|------|------|---------|
| Maven Build | `tools/maven-build.tool.md` | Developer, DevOps Engineer, QA / Test Writer |
| Docker Build | `tools/docker-build.tool.md` | DevOps Engineer |
| GitHub CLI | `tools/github-cli.tool.md` | DevOps Engineer, Project Manager, Orchestrator |
| Dependency Check | `tools/dependency-check.tool.md` | Security Reviewer |
| OpenAPI Validator | `tools/openapi-validator.tool.md` | API Reviewer, Developer |
| Sonar Analysis | `tools/sonar-analysis.tool.md` | Code Reviewer, DevOps Engineer |
| Git | `tools/git.tool.md` | Developer, Orchestrator, DevOps Engineer |
| PMD and Qodana | `tools/pmd-qodana.tool.md` | Code Reviewer, Developer |

See [`tools/tools.md`](tools/tools.md) for full catalog and output locations.

## Prompts

| Prompt | Agent | Mode |
|--------|-------|------|
| Full Feature Delivery | Orchestrator | agent |
| Implement Feature | Developer | agent |
| Fix Bug | Developer | agent |
| Fix Lint Violations | Code Reviewer | agent |
| Write Unit Tests | QA / Test Writer | agent |
| Improve Coverage | QA / Test Writer | agent |
| Review Code | Code Reviewer | agent |
| Security Audit | Security Reviewer | agent |
| Prepare Release | DevOps Engineer | agent |
| Define Story | Product Owner | ask |
| Review API Contract | API Reviewer | agent |
| Bootstrap Agent Infrastructure | Orchestrator | agent |

See [`prompts/prompts.md`](prompts/prompts.md) for the full catalog with trigger conditions.

## Hooks

| Hook | Trigger | Blocking | Agents |
|------|---------|---------|--------|
| Pre Pull Request Gate | PR to `main`/`develop` | **Yes** | code-reviewer, api-reviewer, security-reviewer |
| Post Implementation Review | Push to `feature/*`/`fix/*` | No | test-writer, code-reviewer |
| Post Merge Security Check | Merge to `develop` (pom.xml changed) | No | security-reviewer |
| Pre Release Gate | Before tag `v*` | **Yes** | security-reviewer, api-reviewer, code-reviewer, devops-engineer |

See [`hooks/hooks.md`](hooks/hooks.md) for lifecycle diagram and prompt references.

## Agent Collaboration Map

```
                    ┌──────────────────┐
                    │   Orchestrator   │  ← user-invocable entry point
                    └────────┬─────────┘
                             │ delegates via run_subagent
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
     ┌─────────────┐ ┌────────────┐ ┌──────────────┐
     │Product Owner│ │  Developer │ │QA/Test Writer│
     └──────┬──────┘ └─────┬──────┘ └──────┬───────┘
            │               │               │
            │  requirements │ implementation│ tests
            │               │               │
            ▼               ▼               ▼
     ┌──────────────────────────────────────────┐
     │              Subagents                    │
     │                                           │
     │  ┌─────────────┐  ┌──────────────────┐   │
     │  │API Reviewer  │  │Security Reviewer │   │
     │  └─────────────┘  └──────────────────┘   │
     │  ┌─────────────┐  ┌──────────────────┐   │
     │  │Code Reviewer │  │Database Architect│   │
     │  └─────────────┘  └──────────────────┘   │
     │  ┌──────────────────┐                     │
     │  │ DevOps Engineer  │                     │
     │  └──────────────────┘                     │
     └──────────────────────────────────────────┘
```

## Quick Start

| Use Case | Invoke |
|----------|--------|
| New feature (end-to-end) | Orchestrator → `full-feature-delivery.prompt.md` |
| Implement a single endpoint | Developer → `implement-feature.prompt.md` |
| Fix a bug | Developer → `fix-bug.prompt.md` |
| Write tests | QA / Test Writer → `write-unit-tests.prompt.md` |
| Boost coverage | QA / Test Writer → `improve-coverage.prompt.md` |
| Review a PR | Code Reviewer → `review-code.prompt.md` |
| Security check | Security Reviewer → `security-audit.prompt.md` |
| Prepare a release | DevOps Engineer → `prepare-release.prompt.md` |
| Define a story | Product Owner → `define-story.prompt.md` |

## Infrastructure Counts

- **11** agents (`agents/`)
- **11** per-agent skill definitions (`skills/<agent>/SKILL.md`) + **35** shared skills (`skills/*.skill.md`)
- **8** canonical tools (`tools/`) + 1 deprecated (`maven.md`)
- **12** prompts (`prompts/`)
- **4** hooks (`hooks/`)
