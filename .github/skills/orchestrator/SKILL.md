---
name: Orchestrator Skills
description: Consolidated skill set for orchestrating multi-agent delivery in directory-backend
applies-to: [Orchestrator]
---

# Orchestrator — Skill Definition

## 1. Project-Specific Patterns

- Decompose requests by domain module: User, Business, Campaign, Category, Product, DigitalContact, Favorite, Timezone, Auth.
- Respect request flow: Controller → Service → Repository.
- Quality gates: JaCoCo ≥ 80% line coverage, PMD 0 violations (`ruleset.xml`), `mvn -B -V -e clean verify` passes.
- Never break `/api/v1/*` backward compatibility without explicit user approval.

## 2. Naming Conventions

- Agent delegation references: use exact agent names (`developer`, `test-writer`, `code-reviewer`, `api-reviewer`, `security-reviewer`, `database-architect`, `devops-engineer`, `product-owner`, `project-manager`, `repo-requirements-analyst`).
- Prompt files: `.github/prompts/*.prompt.md`
- Hook files: `.github/hooks/*.hook.md`

## 3. Delegation Protocol

1. Analyze → identify affected layers and domain modules.
2. Plan → list tasks with agent assignments and dependencies.
3. Delegate → call `run_subagent` per task in dependency order.
4. Validate → run `mvn -B -V -e clean verify` after implementation.
5. Report → produce structured delivery summary.

## 4. Key Files

- `.github/agents/*.agent.md` — agent definitions
- `.github/prompts/*.prompt.md` — execution templates
- `.github/hooks/*.hook.md` — trigger definitions
- `src/main/resources/api/index.yaml` — OpenAPI spec
- `AGENTS.md`, `CLAUDE.md` — project conventions

## 5. Constraints

- Never implement code directly.
- Never merge breaking API changes without confirmation.
- Never commit secrets or touch `src/main/resources/*.crt` / `keystore.jks`.

## 6. Checklist

- [ ] Request decomposed into atomic tasks per agent
- [ ] Dependency order defined
- [ ] All subagent calls made via `run_subagent`
- [ ] `mvn -B -V -e clean verify` passes
- [ ] Delivery summary produced with quality gate table

