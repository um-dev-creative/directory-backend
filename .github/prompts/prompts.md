# Prompts Catalog

This folder contains executable prompt templates for AI agents working on `directory-backend`.

## All Prompts

| Prompt | File | Agent | Mode | Trigger |
|--------|------|-------|------|---------|
| Full Feature Delivery | `full-feature-delivery.prompt.md` | orchestrator | agent | New feature story requiring multi-agent coordination |
| Implement Feature | `implement-feature.prompt.md` | developer | agent | New feature or enhancement |
| Fix Bug | `fix-bug.prompt.md` | developer | agent | Bug report |
| Fix Lint Violations | `fix-lint-violations.prompt.md` | code-reviewer | agent | PMD violations in CI or pre-PR |
| Write Unit Tests | `write-unit-tests.prompt.md` | test-writer | agent | New code without tests, or coverage gap |
| Improve Coverage | `improve-coverage.prompt.md` | test-writer | agent | JaCoCo coverage below 80% gate |
| Review Code | `review-code.prompt.md` | code-reviewer | agent | PR opened, push to feature branch |
| Security Audit | `security-audit.prompt.md` | security-reviewer | agent | `pom.xml` changed, pre-release |
| Prepare Release | `prepare-release.prompt.md` | devops-engineer | agent | Release tag creation |
| Define Story | `define-story.prompt.md` | product-owner | ask | New feature request |
| Review API Contract | `review-api-contract.prompt.md` | api-reviewer | agent | `*Api.java` or `index.yaml` changed |
| Bootstrap Agent Infrastructure | `bootstrap-agent-infrastructure.prompt.md` | orchestrator | agent | Initial or periodic agent infra setup |

## Input Variable Convention

All prompts use `${variableName}` placeholders. Substitute before invoking.

## Mode Reference

| Mode | Behavior |
|------|---------|
| `agent` | Agent executes all steps autonomously |
| `ask` | Agent asks clarifying questions before proceeding |
| `edit` | Agent makes targeted code edits only |

