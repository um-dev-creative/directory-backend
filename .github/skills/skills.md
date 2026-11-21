# Skills Catalog

This folder catalogs reusable skills that AI agents can call as part of their work in
`directory-backend`. Each skill file uses front matter plus markdown sections.

## Per-Agent Skill Definitions

Each agent has a dedicated `SKILL.md` in its own subfolder that consolidates project-specific patterns, naming conventions, error handling, key files, constraints, and a checklist.

| Agent | Skill Definition File |
|---|---|
| Orchestrator | `orchestrator/SKILL.md` |
| Developer | `developer/SKILL.md` |
| QA / Test Writer | `test-writer/SKILL.md` |
| Code Reviewer | `code-reviewer/SKILL.md` |
| Security Reviewer | `security-reviewer/SKILL.md` |
| DevOps Engineer | `devops-engineer/SKILL.md` |
| API Reviewer | `api-reviewer/SKILL.md` |
| Database Architect | `database-architect/SKILL.md` |
| Product Owner | `product-owner/SKILL.md` |
| Project Manager | `project-manager/SKILL.md` |
| Repo Requirements Analyst | `repo-requirements-analyst/SKILL.md` |

## Shared Skill Library

The flat `*.skill.md` files are shared skills referenced by multiple agents.

| Skill | File | Used By |
|---|---|---|
| Acceptance Criteria | `acceptance-criteria.skill.md` | Product Owner, Repo Requirements Analyst |
| API Contract Review | `api-contract-review.skill.md` | API Reviewer, Product Owner, Developer |
| Backlog Management | `backlog-management.skill.md` | Product Owner, Project Manager, Repo Requirements Analyst |
| CI/CD Orchestration | `ci-cd-orchestration.skill.md` | Project Manager, DevOps Engineer |
| Clean Code | `clean-code.skill.md` | Code Reviewer |
| CVE Detection | `cve-detection.skill.md` | Security Reviewer |
| Dependency Audit | `dependency-audit.skill.md` | Security Reviewer |
| Docker Containerization | `docker-containerization.skill.md` | DevOps Engineer |
| Feign Integration | `feign-integration.skill.md` | Developer |
| GitHub Actions | `github-actions.skill.md` | DevOps Engineer |
| JaCoCo Coverage | `jacoco-coverage.skill.md` | QA / Test Writer, Project Manager |
| Java Code Quality | `java-code-quality.skill.md` | Code Reviewer |
| Java Spring Development | `java-spring-development.skill.md` | Developer, Code Reviewer |
| JPA Persistence | `jpa-persistence.skill.md` | Developer, Database Architect |
| JUnit 5 Testing | `junit5-testing.skill.md` | QA / Test Writer, Developer |
| JWT Security | `jwt-security.skill.md` | Security Reviewer |
| Kafka Messaging | `kafka-messaging.skill.md` | Developer |
| MapStruct Mapping | `mapstruct-mapping.skill.md` | Developer |
| Maven Build | `maven-build.skill.md` | DevOps Engineer |
| Mockito Mocking | `mockito-mocking.skill.md` | QA / Test Writer |
| OpenAPI Specification | `openapi-specification.skill.md` | Product Owner, API Reviewer |
| OWASP Top 10 | `owasp-top10.skill.md` | Security Reviewer |
| PMD Analysis | `pmd-analysis.skill.md` | Code Reviewer |
| Quality Gates | `quality-gates.skill.md` | Project Manager |
| Release Management | `release-management.skill.md` | Project Manager, DevOps Engineer |
| REST API Design | `rest-api-design.skill.md` | Developer, API Reviewer, Product Owner |
| Risk Assessment | `risk-assessment.skill.md` | Project Manager, Repo Requirements Analyst |
| Schema Design | `schema-design.skill.md` | Database Architect |
| Spring Boot Best Practices | `spring-boot-best-practices.skill.md` | Code Reviewer |
| Spring Boot Testing | `spring-boot-testing.skill.md` | QA / Test Writer |
| Spring Data JPA | `spring-data-jpa.skill.md` | Database Architect |
| Spring Security | `spring-security.skill.md` | Security Reviewer |
| Sprint Planning | `sprint-planning.skill.md` | Project Manager |
| SQL Optimization | `sql-optimization.skill.md` | Database Architect |
| Test Design Patterns | `test-design-patterns.skill.md` | QA / Test Writer |

Keep both tables synchronized with `agents/*.agent.md` whenever skill references are added or removed.
