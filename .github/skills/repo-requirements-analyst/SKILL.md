---
name: Repo Requirements Analyst Skills
description: Consolidated skill set for codebase analysis and requirement discovery in directory-backend
applies-to: [Repo Requirements Analyst]
---

# Repo Requirements Analyst — Skill Definition

## 1. Project-Specific Patterns

- Entry point: `DirectoryBackendApplication.java` — `@EnableFeignClients`, scans `com.umdc.commons.services`, `com.umdc.directory`, `com.umdc.security`.
- Domain modules discoverable via package scan: User, Business, Campaign, Category, Product, DigitalContact, Favorite, Timezone, Auth.
- Request flow: Controller → Service → Repository.
- External integrations: Backbone Feign client, Mercury Feign client, HashiCorp Vault, Spring Cloud Config, Kafka.

## 2. Analysis Outputs

- Package dependency maps.
- Domain entity relationship summaries.
- Gap analysis: missing endpoints, uncovered scenarios, deprecated patterns.
- Risk register: breaking changes, security exposures, coverage gaps.

## 3. Key Files

- `src/main/java/com/prx/directory/` — entire source tree
- `src/main/resources/api/index.yaml` — current API surface
- `AGENTS.md`, `CLAUDE.md` — project conventions
- `pom.xml` — dependency and plugin inventory

## 4. Constraints

- Do not modify source files — analysis only.
- Surface findings as structured reports with file:line references.

## 5. Checklist

- [ ] Entry point and scan packages identified
- [ ] All domain modules enumerated
- [ ] API endpoints inventoried from `index.yaml`
- [ ] External integration boundaries documented
- [ ] Risk items flagged with severity (High/Medium/Low)
- [ ] Findings report produced in markdown

