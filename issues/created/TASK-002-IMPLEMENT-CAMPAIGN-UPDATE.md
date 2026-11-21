# TASK-002: Implement Campaign Update Functionality

## Priority
**Medium**

## Status
Open

## Component
- Campaign Service
- Package: `com.umdc.directory.api.v1.service`
- Class: `CampaignService` (interface)
- Implementation: `CampaignServiceImpl`

## Description
The `CampaignService.update(UUID id, CampaignUpdateRequest request)` method is currently a default implementation returning `HttpStatus.NOT_IMPLEMENTED`. This prevents users from updating existing campaigns via the API.

## Current Behavior
- Client sends `PUT /api/v1/campaigns/{id}` with update request
- Service returns HTTP 501 Not Implemented
- No campaign updates are possible

## Expected Behavior
- Client sends `PUT /api/v1/campaigns/{id}` with `CampaignUpdateRequest`
- Service validates:
  - Campaign exists (404 if not found)
  - Category exists if categoryId provided (400 if invalid)
  - Business exists if businessId provided (400 if invalid)
  - Start date <= end date if both provided (400 if invalid)
  - Optimistic locking check using `lastUpdate` timestamp (409 if conflict)
- Service updates only provided fields (partial update support)
- Service returns HTTP 202 Accepted with `CampaignUpdateResponse` (id + new lastUpdate timestamp)

## Acceptance Criteria
1. **Update campaign with valid data**
   - Given: Valid campaign ID and update request
   - When: PUT request is sent
   - Then: Campaign is updated and HTTP 202 returned with updated `lastUpdate`

2. **Campaign not found**
   - Given: Non-existent campaign ID
   - When: PUT request is sent
   - Then: HTTP 404 Not Found returned

3. **Invalid foreign keys**
   - Given: Update request with non-existent categoryId or businessId
   - When: PUT request is sent
   - Then: HTTP 400 Bad Request returned

4. **Invalid date range**
   - Given: startDate > endDate in update request
   - When: PUT request is sent
   - Then: HTTP 400 Bad Request returned

5. **Optimistic locking conflict**
   - Given: Campaign has been modified since client last read it
   - When: PUT request is sent with outdated `lastUpdate`
   - Then: HTTP 409 Conflict returned

6. **Partial updates**
   - Given: Update request with only some fields (e.g., just title)
   - When: PUT request is sent
   - Then: Only specified fields are updated, others remain unchanged

## Technical Tasks
- [ ] Implement `CampaignServiceImpl.update(...)` method
- [ ] Add foreign key validation (category, business existence checks)
- [ ] Add date range validation (startDate <= endDate)
- [ ] Implement optimistic locking using `lastUpdate` field
- [ ] Support partial updates (only update provided fields)
- [ ] Create/update unit tests in `CampaignServiceImplTest`
- [ ] Update API documentation (`src/main/resources/api/index.yaml`)

## Testing Requirements
Minimum test cases:
1. Successful update with all fields
2. Successful partial update (title only)
3. Successful partial update (dates only)
4. Campaign not found returns 404
5. Invalid categoryId returns 400
6. Invalid businessId returns 400
7. Invalid date range returns 400
8. Optimistic locking conflict returns 409
9. Null request returns 400

## Dependencies
- `CampaignRepository` (already exists)
- `CategoryRepository` (already exists)
- `BusinessRepository` (already exists)
- `CampaignMapper` (already exists)

## Estimated Effort
**6-8 hours**

## References
- Interface: `src/main/java/com/prx/directory/api/v1/service/CampaignService.java`
- Implementation: `src/main/java/com/prx/directory/api/v1/service/CampaignServiceImpl.java`
- Test: `src/test/java/com/prx/directory/api/v1/service/CampaignServiceImplTest.java`
- Documentation: `docs/COMPLETE_PROJECT_DOCUMENTATION_FOR_BA.md`

## Quality Gates
- [ ] Code coverage ≥ 80% line coverage
- [ ] No PMD violations
- [ ] All tests pass
- [ ] SonarCloud quality gate passed
- [ ] OpenAPI spec updated

