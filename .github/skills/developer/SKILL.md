---
name: Developer Skills
description: Consolidated skill set for feature/fix implementation in directory-backend
applies-to: [Developer]
---

# Developer — Skill Definition

## 1. Project-Specific Patterns

- **API split**: `*Api.java` (Swagger annotations + default methods) ← thin `*Controller.java` implementation.
- **Service layer**: `*ServiceImpl.java` owns validation, status logic, returns `ResponseEntity` directly.
- **DTO layer**: records in `com.umdc.directory.api.v1.to` — immutable, no setters.
- **Mapper layer**: MapStruct interfaces in `com.umdc.directory.mapper`, using shared `MapperAppConfig` from `com.umdc.commons.services.config.mapper`.
- **Filtering**: `CampaignFilterParser` → `CampaignSpecifications` → paged repository query; legacy aliases (`q`, `category_id`, `business_id`, `start_date_*`, `end_date_*`) must be preserved.
- **Optimistic lock**: `CampaignServiceImpl.update()` checks `lastUpdate` → returns `202 Accepted`.
- **Headers**: always use `DirectoryAppConstants.MESSAGE_HEADER` and `MESSAGE_ERROR_HEADER`.

## 2. Naming Conventions

| Artifact | Pattern | Example |
|----------|---------|---------|
| API interface | `*Api.java` | `CampaignApi.java` |
| Controller | `*Controller.java` | `CampaignController.java` |
| Service interface | `*Service.java` | `CampaignService.java` |
| Service impl | `*ServiceImpl.java` | `CampaignServiceImpl.java` |
| DTO (request) | `*Request.java` | `CampaignRequest.java` |
| DTO (response) | `*Response.java` or `*To.java` | `CampaignResponse.java` |
| Mapper | `*Mapper.java` | `CampaignMapper.java` |
| Entity | `*Entity.java` | `CampaignEntity.java` |
| Repository | `*Repository.java` | `CampaignRepository.java` |
| Specification | `*Specifications.java` | `CampaignSpecifications.java` |

## 3. Error Handling

- Use `ResponseEntity` with appropriate HTTP status: `200 OK`, `201 Created`, `202 Accepted`, `204 No Content`, `400 Bad Request`, `404 Not Found`, `409 Conflict`.
- Set `MESSAGE_HEADER` / `MESSAGE_ERROR_HEADER` on all responses.
- Validation errors → `400` with descriptive header.
- Not-found → `404`.
- Update with stale `lastUpdate` → `409 Conflict`.

## 4. Key Files

- `src/main/java/com/prx/directory/api/v1/controller/` — controllers + API interfaces
- `src/main/java/com/prx/directory/api/v1/service/` — service impls
- `src/main/java/com/prx/directory/api/v1/to/` — DTOs
- `src/main/java/com/prx/directory/jpa/entity/` — entities
- `src/main/java/com/prx/directory/mapper/` — MapStruct mappers
- `src/main/java/com/prx/directory/constant/DirectoryAppConstants.java` — constants
- `src/main/resources/api/index.yaml` — OpenAPI spec (update together with `*Api.java`)

## 5. Constraints

- Do not modify `*.crt`, `keystore.jks`.
- Do not commit secrets (`default.env`).
- Do not refactor unrelated packages — atomic, minimal edits.
- Always update both `*Api.java` annotations and `index.yaml` when contracts change.

## 6. Checklist

- [ ] `*Api.java` annotation updated if contract changed
- [ ] `src/main/resources/api/index.yaml` updated if contract changed
- [ ] `DirectoryAppConstants` used for new string literals
- [ ] `MESSAGE_HEADER` set on all response entities
- [ ] `ResponseEntity` returned from service, not just domain objects
- [ ] `mvn -DskipTests package` compiles clean
- [ ] `mvn test` passes
- [ ] JaCoCo coverage ≥ 80%

