---
name: Database Architect Skills
description: Consolidated skill set for JPA entity design and schema management in directory-backend
applies-to: [Database Architect]
---

# Database Architect — Skill Definition

## 1. Project-Specific Patterns

- ORM: Spring Data JPA + Hibernate; entities in `com.umdc.directory.jpa.entity`.
- Composite keys: use `@EmbeddedId` pattern (e.g., `BusinessProductEntityId`).
- Repositories: `JpaRepository` + `JpaSpecificationExecutor` for dynamic filtering.
- Specifications: `*Specifications.java` classes compose `Predicate` via `CriteriaBuilder`.
- Liquibase/Flyway: check `pom.xml` — if present, all schema changes go through migration scripts.
- Test DB: H2 in-memory with `application-test.yml` — validate DDL compatibility.

## 2. Naming Conventions

| Artifact | Pattern | Example |
|----------|---------|---------|
| Entity | `*Entity.java` | `CampaignEntity.java` |
| Composite key | `*EntityId.java` | `BusinessProductEntityId.java` |
| Repository | `*Repository.java` | `CampaignRepository.java` |
| Specification | `*Specifications.java` | `CampaignSpecifications.java` |
| Filter parser | `*FilterParser.java` | `CampaignFilterParser.java` |

## 3. Error Handling

- Flag: missing `@Column(nullable = false)` on required fields.
- Flag: N+1 query issues — recommend `@EntityGraph` or `JOIN FETCH`.
- Flag: schema changes without migration script.

## 4. Key Files

- `src/main/java/com/prx/directory/jpa/entity/` — all entities
- `src/main/java/com/prx/directory/jpa/repository/` — repositories
- `src/main/java/com/prx/directory/jpa/spec/` — specifications

## 5. Constraints

- Do not alter existing column names or types without a migration script and backward-compat analysis.
- H2 compatibility required for test profile.

## 6. Checklist

- [ ] Entity class annotated with `@Entity` and `@Table(name = "...")`
- [ ] Composite keys use `@EmbeddedId`
- [ ] All required fields have `@Column(nullable = false)`
- [ ] Repository extends `JpaSpecificationExecutor` if filtering is needed
- [ ] Schema change has accompanying migration script
- [ ] H2-compatible DDL tested with `application-test.yml`

