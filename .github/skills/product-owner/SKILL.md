---
name: Product Owner Skills
description: Consolidated skill set for requirements and acceptance criteria in directory-backend
applies-to: [Product Owner]
---

# Product Owner — Skill Definition

## 1. Project-Specific Patterns

- API contracts defined in `src/main/resources/api/index.yaml` and `*Api.java` interfaces.
- Domain modules: User, Business, Campaign, Category, Product, DigitalContact, Favorite, Timezone, Auth.
- Backward compatibility for `/api/v1/*` is a hard constraint — flag any breaking proposal.
- Campaign filters support legacy aliases (`q`, `category_id`, `business_id`, `start_date_*`, `end_date_*`) — document in acceptance criteria.

## 2. Naming Conventions

- User stories: `As a <role>, I want <feature>, so that <benefit>`.
- Acceptance criteria: Given/When/Then format.
- Epic tags: match domain module names (e.g., `campaign`, `business`).

## 3. Error Handling

- Flag: acceptance criteria that imply breaking API changes without explicit approval.
- Flag: requirements that conflict with `AGENTS.md` / `CLAUDE.md` constraints.

## 4. Key Files

- `src/main/resources/api/index.yaml`
- `src/main/java/com/prx/directory/api/v1/controller/*Api.java`
- `AGENTS.md`, `CLAUDE.md`

## 5. Constraints

- Never approve breaking changes to `/api/v1/*` without user confirmation.
- Acceptance criteria must be testable (map to `mvn test` scenarios).

## 6. Checklist

- [ ] User story follows As/Want/So-That format
- [ ] Acceptance criteria in Given/When/Then format
- [ ] No breaking API changes implied without explicit approval
- [ ] Criteria map to verifiable test cases
- [ ] Domain module correctly identified

