# TASK-007: Implement Sorting Functionality in Favorite Service

## Priority
**Medium**

## Status
Open

## Component
- Favorite Service
- Package: `com.umdc.directory.api.v1.service`
- Class: `FavoriteServiceImpl`
- Method: `getFavorites(String token, String type, String sort, Integer page, Integer perPage)`

## Description
The `FavoriteServiceImpl.getFavorites()` method currently returns `HttpStatus.NOT_IMPLEMENTED` when a sort parameter is provided. Users cannot sort their favorites list, limiting usability.

## Current Behavior
```java
if (sort != null) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
}
```
- Any request with `?sort=...` parameter returns HTTP 501 Not Implemented
- Sorting is not available for favorites

## Expected Behavior
- Support sorting favorites by common fields:
  - `createdDate` (when favorite was added) - ascending/descending
  - `productName` / `businessName` / `offerName` (name of favorited item)
  - `type` (STORE, PRODUCT, OFFER)
  - `active` (active status of favorited item)
- Sort parameter format: `field,direction` (e.g., `createdDate,desc`)
- Default sort: `createdDate,desc` (most recent first) if no sort specified
- Multiple sort fields: `field1,dir1;field2,dir2`

## Acceptance Criteria
1. **Sort by created date descending**
   - Given: User favorites with different creation dates
   - When: `GET /api/v1/favorites?sort=createdDate,desc`
   - Then: Favorites returned newest first

2. **Sort by created date ascending**
   - Given: User favorites with different creation dates
   - When: `GET /api/v1/favorites?sort=createdDate,asc`
   - Then: Favorites returned oldest first

3. **Sort by name**
   - Given: User favorites with different names
   - When: `GET /api/v1/favorites?sort=name,asc`
   - Then: Favorites returned alphabetically by item name

4. **Sort by type**
   - Given: Mix of STORE, PRODUCT, OFFER favorites
   - When: `GET /api/v1/favorites?sort=type,asc`
   - Then: Favorites grouped by type

5. **Invalid sort field**
   - Given: Invalid sort field (e.g., `invalidField,asc`)
   - When: GET request sent
   - Then: HTTP 400 Bad Request returned

6. **Invalid sort direction**
   - Given: Invalid direction (not `asc` or `desc`)
   - When: GET request sent
   - Then: HTTP 400 Bad Request returned

7. **Combine sorting and filtering**
   - Given: `?type=PRODUCT&sort=createdDate,desc`
   - When: GET request sent
   - Then: Only PRODUCT favorites returned, sorted by date descending

8. **Combine sorting and pagination**
   - Given: `?sort=name,asc&page=1&perPage=10`
   - When: GET request sent
   - Then: First 10 favorites sorted by name

## Technical Tasks
- [ ] Remove `if (sort != null) return NOT_IMPLEMENTED` check
- [ ] Implement sort parameter parsing (field + direction)
- [ ] Map sort fields to entity fields
- [ ] Build `Pageable` with `Sort` object
- [ ] Handle invalid sort fields (return 400)
- [ ] Handle invalid sort directions (return 400)
- [ ] Support multiple sort fields
- [ ] Add default sort if none provided
- [ ] Update unit tests in `FavoriteServiceImplTest`
- [ ] Update API documentation

## Testing Requirements
Minimum test cases:
1. Sort by createdDate descending
2. Sort by createdDate ascending
3. Sort by name ascending
4. Sort by type ascending
5. Invalid sort field returns 400
6. Invalid sort direction returns 400
7. Null sort uses default
8. Combine sort + filter (type=PRODUCT)
9. Combine sort + pagination

## Dependencies
- `FavoriteRepository` (already supports `Pageable` with `Sort`)
- Sort parameter parser utility (may need to create)

## Estimated Effort
**4-6 hours**
- Implement sort parsing: 2 hours
- Implement sort application: 1 hour
- Add validation: 1 hour
- Write tests: 2 hours

## References
- Implementation: `src/main/java/com/prx/directory/api/v1/service/FavoriteServiceImpl.java`
- Test: `src/test/java/com/prx/directory/api/v1/service/FavoriteServiceImplTest.java`
- Similar implementation: `CampaignServiceImpl.list()` has sort logic to reference

## Quality Gates
- [ ] Code coverage ≥ 80% line coverage
- [ ] No PMD violations
- [ ] All tests pass
- [ ] API documentation updated

## Business Value
Users can organize their favorites by:
- Most recent (see latest additions)
- Alphabetical (easy browsing)
- Type (group stores, products, offers together)

This improves user experience and makes the favorites feature more useful.

## Notes
Consider reusing sort parsing logic from `CampaignServiceImpl` if applicable, or create a shared utility class for sort parameter parsing to avoid code duplication.

