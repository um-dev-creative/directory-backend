# TASK-010: Formalize API Versioning Strategy and Documentation

## Priority
**Medium**

## Status
Open

## Component
- API Design & Documentation
- Package: `com.umdc.directory.api`
- Documentation: `docs/API_VERSIONING_GUIDE.md` (to be created)

## Description
The API currently uses `/api/v1/*` URL-based versioning, but there's no formal documentation of the versioning strategy, deprecation policy, or guidelines for introducing breaking changes. This creates risk when evolving the API.

## Current State
- All endpoints are under `/api/v1/*`
- No documented versioning policy
- No deprecation headers or warnings
- No clear process for introducing v2
- Backward compatibility maintained ad-hoc

## Expected Outcome
A comprehensive API versioning strategy that includes:
1. **Versioning Approach** - URI-based (`/api/v1`, `/api/v2`)
2. **Breaking vs Non-Breaking Changes** - Clear definitions
3. **Deprecation Policy** - Timeline and process
4. **Migration Support** - How to help clients migrate
5. **Version Lifecycle** - Support windows for each version

## Proposed Versioning Strategy

### Versioning Method
**URI-Based Versioning** (current approach)
- Format: `/api/v{major-version}/{resource}`
- Example: `/api/v1/campaigns`, `/api/v2/campaigns`
- Major version in URI, minor/patch via headers

### Version Numbering
- **Major version** (v1, v2, v3) - Breaking changes
- **Minor version** (header: `API-Version: 1.2`) - New features, backward compatible
- **Patch version** (internal) - Bug fixes, no API changes

### Breaking Changes (Require Major Version Bump)
- Removing an endpoint
- Removing a request/response field
- Changing field types
- Changing authentication mechanism
- Renaming fields
- Changing error response structure

### Non-Breaking Changes (Minor Version Bump)
- Adding new endpoints
- Adding new optional request fields
- Adding new response fields
- Adding new query parameters (optional)
- Deprecating (but not removing) fields
- Fixing bugs without contract changes

### Deprecation Process
1. **Announce** - Add deprecation header: `Sunset: Sat, 31 Dec 2026 23:59:59 GMT`
2. **Warn** - Log warnings for deprecated endpoint usage
3. **Document** - Update OpenAPI spec with deprecation notice
4. **Migrate** - Provide migration guide
5. **Remove** - After minimum 6 months support window

### Version Support Policy
- **Current version** (v2) - Full support, new features
- **Previous version** (v1) - Bug fixes only, 12 months after v2 release
- **Deprecated versions** (v0) - 6 months notice before removal

## Acceptance Criteria
1. **Documentation created**
   - Create `docs/API_VERSIONING_GUIDE.md`
   - Document versioning strategy
   - Document deprecation process
   - Provide examples of breaking vs non-breaking changes

2. **Deprecation headers implemented**
   - Add `Sunset` header to deprecated endpoints
   - Add `API-Deprecated` header with migration info
   - Log usage of deprecated endpoints

3. **OpenAPI spec updated**
   - Mark deprecated endpoints with `deprecated: true`
   - Add version information to API spec
   - Document version history

4. **Migration guide template**
   - Create template for v1 → v2 migration
   - Include breaking changes checklist
   - Provide code examples

5. **Version header support**
   - Accept `API-Version` header for minor version selection
   - Default to latest minor version if not specified
   - Return current version in `API-Version` response header

## Technical Tasks
- [ ] Create `docs/API_VERSIONING_GUIDE.md`
- [ ] Document versioning strategy
- [ ] Implement deprecation header interceptor
- [ ] Add version response header to all endpoints
- [ ] Update OpenAPI spec with version info
- [ ] Create migration guide template
- [ ] Add version logging and metrics
- [ ] Create developer guide for adding new API versions
- [ ] Add version detection in integration tests
- [ ] Update CLAUDE.md and AGENTS.md with versioning policy

## Documentation Structure

### docs/API_VERSIONING_GUIDE.md
```markdown
# API Versioning Guide

## Overview
## Versioning Strategy
## Version Numbering
## Breaking vs Non-Breaking Changes
## Deprecation Policy
## Version Lifecycle
## Migration Process
## Examples
## FAQ
```

## Deprecation Header Example
```http
HTTP/1.1 200 OK
Sunset: Sat, 31 Dec 2026 23:59:59 GMT
API-Deprecated: true
API-Deprecated-Message: This endpoint is deprecated. Use /api/v2/campaigns instead.
Link: </docs/migration/v1-to-v2>; rel="sunset"
```

## Testing Requirements
- Add test verifying deprecation headers are present
- Add test verifying version headers are returned
- Add test for version header parsing (minor version selection)
- Integration test for deprecated endpoint logging

## Dependencies
- None (documentation and policy)

## Estimated Effort
**6-8 hours**
- Research best practices: 1 hour
- Write documentation: 3 hours
- Implement header support: 2 hours
- Testing and validation: 2 hours

## References
- RFC 8594 (Sunset Header)
- API Evolution Best Practices
- Semantic Versioning (semver.org)
- RESTful API Versioning Patterns

## Quality Gates
- [ ] Documentation reviewed and approved
- [ ] All deprecation headers implemented
- [ ] OpenAPI spec updated
- [ ] Migration guide template created
- [ ] Tests pass

## Business Value
- **Clarity** for API consumers on version lifecycle
- **Confidence** to evolve API without breaking clients
- **Professionalism** - clear communication of changes
- **Reduced support burden** - clients know what to expect

## Communication Plan
1. Announce versioning policy in release notes
2. Email API consumers with deprecation timeline
3. Update developer portal with versioning guide
4. Monitor deprecated endpoint usage
5. Provide support during migration period

## Example Migration Scenario

### Scenario: Renaming `business_id` to `businessId`
1. **v1.0** - Original field `business_id`
2. **v1.1** - Add `businessId`, keep `business_id` (both work)
3. **v1.2** - Deprecate `business_id`, add `Sunset` header
4. **v2.0** - Remove `business_id`, only `businessId` supported

## Notes
This task is primarily documentation and policy definition, with some implementation for deprecation headers. It's crucial for long-term API maintainability and client relationships.

