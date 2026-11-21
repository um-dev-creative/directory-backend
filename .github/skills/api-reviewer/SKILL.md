---
name: API Reviewer Skills
description: Consolidated skill set for OpenAPI contract review in directory-backend
applies-to: [API Reviewer]
---

# API Reviewer — Skill Definition

## 1. Project-Specific Patterns

- OpenAPI spec: `src/main/resources/api/index.yaml` — must be kept in sync with `*Api.java` annotations.
- Validator: `mvn springdoc-openapi:generate` or Swagger CLI against `index.yaml`.
- All endpoints live under `/api/v1/*` — backward compatibility is mandatory.
- API annotations in `*Api.java` (e.g., `CampaignApi.java`); controllers are thin delegators.
- Response schema must match DTOs in `com.umdc.directory.api.v1.to`.

## 2. Naming Conventions

- Path convention: `/api/v1/{domain}` (e.g., `/api/v1/campaign`, `/api/v1/business`).
- Operation ID convention: `<verb><Domain>` (e.g., `listCampaigns`, `createBusiness`).
- Schema names: match DTO class names (e.g., `CampaignRequest`, `CampaignResponse`).

## 3. Error Handling

- Flag: response schema missing `MESSAGE_HEADER` header definition.
- Flag: breaking changes to existing path/method/required-field combinations.
- Flag: `index.yaml` out of sync with `*Api.java` annotations.

## 4. Key Files

- `src/main/resources/api/index.yaml`
- `src/main/java/com/prx/directory/api/v1/controller/*Api.java`
- `src/main/java/com/prx/directory/api/v1/to/` — DTO schemas
- `.github/tools/openapi-validator.tool.md`

## 5. Constraints

- Never approve a breaking change to `/api/v1/*` without explicit user confirmation.
- Always verify both `*Api.java` and `index.yaml` are updated together.

## 6. Checklist

- [ ] `index.yaml` validates with no errors (`swagger-cli validate`)
- [ ] New/changed paths match `*Api.java` annotations exactly
- [ ] Request/response schemas match DTOs in `api/v1/to`
- [ ] No existing required fields removed or renamed
- [ ] HTTP status codes consistent with project patterns
- [ ] `MESSAGE_HEADER` documented in response headers section

