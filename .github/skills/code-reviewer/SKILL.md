---
name: Code Reviewer Skills
description: Consolidated skill set for code quality review in directory-backend
applies-to: [Code Reviewer]
---

# Code Reviewer — Skill Definition

## 1. Project-Specific Patterns

- Static analysis: PMD 3.28.0 with `ruleset.xml` at project root — run `mvn pmd:check`.
- SonarCloud: `mvn sonar:sonar` (requires `SONAR_TOKEN`).
- Qodana: config in `qodana.yaml`, results in `qodana.sarif.json`.
- JaCoCo: 80% line coverage gate enforced at `mvn verify`.
- Conventions enforced: `*Api.java` / `*Controller.java` split, `ResponseEntity` returns from service, `DirectoryAppConstants` for literals.

## 2. Naming Conventions

- See developer SKILL.md for full naming table.
- Flag violations: direct string literals where `DirectoryAppConstants` exists, business logic in controllers, domain objects returned raw (not wrapped in `ResponseEntity`).

## 3. Error Handling

- Flag: unchecked exceptions escaping service layer without `ResponseEntity` wrapping.
- Flag: missing `MESSAGE_HEADER` / `MESSAGE_ERROR_HEADER` on responses.
- Flag: HTTP status mismatches (e.g., returning `200` for resource creation instead of `201`).

## 4. Key Files

- `ruleset.xml` — PMD ruleset
- `qodana.yaml` — Qodana config
- `src/main/java/com/prx/directory/constant/DirectoryAppConstants.java`
- `.github/tools/pmd-qodana.tool.md`
- `.github/tools/sonar-analysis.tool.md`

## 5. Constraints

- Do not suggest refactors outside the scope of the PR/change.
- Do not block on style issues that PMD/Qodana do not flag.

## 6. Checklist

- [ ] `mvn pmd:check` passes (0 violations)
- [ ] No raw domain objects returned from service — all wrapped in `ResponseEntity`
- [ ] `DirectoryAppConstants` used instead of new string literals
- [ ] No business logic in `*Controller.java`
- [ ] `MESSAGE_HEADER` set on all response entities
- [ ] JaCoCo ≥ 80% line coverage
- [ ] No secrets or credentials in code

