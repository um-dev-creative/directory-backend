# TASK-005: Add Comprehensive Unit Tests for CampaignServiceImpl

## Priority
**High**

## Status
Open

## Component
- Campaign Service Tests
- Package: `com.umdc.directory.api.v1.service`
- Test Class: `CampaignServiceImplTest`

## Description
While `CampaignServiceImpl` has basic test coverage, comprehensive unit tests covering all edge cases, validation scenarios, and error paths are missing. The service has complex business logic including filtering, pagination, sorting, and foreign key validation that need thorough testing.

## Current Test Coverage Gaps
Based on similar services (`BusinessServiceImplExtendedTest`, `UserServiceImplExtendedTest`), the following scenarios need testing:

### Create Campaign Tests (Missing)
- [ ] Null request returns BAD_REQUEST
- [ ] Null categoryId returns BAD_REQUEST
- [ ] Null businessId returns BAD_REQUEST
- [ ] Non-existent categoryId returns BAD_REQUEST
- [ ] Non-existent businessId returns BAD_REQUEST
- [ ] Invalid date range (startDate > endDate) returns BAD_REQUEST
- [ ] Duplicate campaign name for same business returns CONFLICT
- [ ] Success sets createdDate and lastUpdate
- [ ] Success sets active=true if not provided

### List Campaigns Tests (Missing)
- [ ] Default pagination (page=1, perPage=20)
- [ ] Custom pagination (various page/size combinations)
- [ ] Invalid page number returns BAD_REQUEST
- [ ] Invalid perPage (>100 or <1) returns BAD_REQUEST
- [ ] Sort by title ascending
- [ ] Sort by start_date descending
- [ ] Sort by created_date
- [ ] Filter by business_id (legacy parameter)
- [ ] Filter by businessId (new parameter)
- [ ] Filter by category_id (legacy parameter)
- [ ] Filter by categoryId (new parameter)
- [ ] Filter by active status
- [ ] Filter by search term (q parameter)
- [ ] Filter by start_date_from/to range
- [ ] Filter by end_date_from/to range
- [ ] Combined filters (business + category + date range)
- [ ] Empty results return OK with empty page

### Find Campaign Tests (Partial)
- [x] Success returns OK (exists)
- [x] Not found returns NOT_FOUND (exists)
- [ ] Null id returns BAD_REQUEST

### Update Campaign Tests (Not Implemented)
See TASK-002 for full requirements

## Expected Test Structure

```java
@DisplayName("CampaignServiceImpl - Extended Test Suite")
class CampaignServiceImplExtendedTest {
    
    @Nested
    @DisplayName("Create Campaign Tests")
    class CreateCampaignTests {
        // 10+ test cases
    }
    
    @Nested
    @DisplayName("List Campaigns Tests")
    class ListCampaignsTests {
        // 20+ test cases covering pagination, sorting, filtering
    }
    
    @Nested
    @DisplayName("Find Campaign Tests")
    class FindCampaignTests {
        // 3+ test cases
    }
    
    @Nested
    @DisplayName("Update Campaign Tests")
    class UpdateCampaignTests {
        // 10+ test cases (after TASK-002 is completed)
    }
}
```

## Acceptance Criteria
1. **Minimum 40 test cases** covering all create, list, and find scenarios
2. **80%+ line coverage** for `CampaignServiceImpl`
3. **60%+ branch coverage** for `CampaignServiceImpl`
4. All validation scenarios tested (null checks, FK validation, date validation)
5. All pagination edge cases tested
6. All sorting options tested
7. All filter combinations tested
8. All error paths tested with proper HTTP status codes

## Technical Tasks
- [ ] Create `CampaignServiceImplExtendedTest.java`
- [ ] Implement create campaign test suite (10+ tests)
- [ ] Implement list campaigns test suite (20+ tests)
- [ ] Implement find campaign test suite (3+ tests)
- [ ] Use `@DisplayName` annotations for readable test names
- [ ] Use `@Nested` classes to organize test suites
- [ ] Mock all dependencies (repositories, mappers)
- [ ] Use `ArgumentCaptor` to verify saved entity state
- [ ] Run coverage report and verify ≥80% line coverage

## Testing Requirements
All tests must:
- Use JUnit 5 (`@Test`, `@BeforeEach`, `@Nested`, `@DisplayName`)
- Use Mockito for mocking (`@Mock`, `@InjectMocks`)
- Assert HTTP status codes
- Assert response body content
- Verify repository interactions
- Be independent and isolated
- Follow existing test patterns in the codebase

## Dependencies
- TASK-002 (Update implementation) should be completed before adding update tests
- Existing `CampaignServiceImplTest` can be kept or merged

## Estimated Effort
**12-16 hours**
- Create test suite: 2 hours
- List campaign tests (20+): 6 hours
- Create campaign tests (10+): 4 hours
- Find campaign tests (3+): 1 hour
- Coverage verification and fixes: 3 hours

## References
- Implementation: `src/main/java/com/prx/directory/api/v1/service/CampaignServiceImpl.java`
- Existing tests: `src/test/java/com/prx/directory/api/v1/service/CampaignServiceImplTest.java`
- Test patterns: `src/test/java/com/prx/directory/api/v1/service/BusinessServiceImplExtendedTest.java`
- Filter tests: `src/test/java/com/prx/directory/api/v1/service/CampaignFilterParserTest.java`

## Quality Gates
- [ ] ≥80% line coverage for `CampaignServiceImpl`
- [ ] ≥60% branch coverage for `CampaignServiceImpl`
- [ ] All tests pass
- [ ] No PMD violations
- [ ] JaCoCo bundle coverage ≥80%

## Notes
This task significantly increases test coverage for one of the most complex services in the application. The investment in comprehensive testing will prevent regression bugs and improve maintainability.

