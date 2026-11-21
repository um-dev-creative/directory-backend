# TASK-008: Add Integration Tests for REST Controllers

## Priority
**Medium**

## Status
Open

## Component
- Controller Integration Tests
- Package: `com.umdc.directory.api.v1.controller`
- Test Package: `src/test/java/com/prx/directory/api/v1/controller`

## Description
The project has extensive unit tests for service layer but lacks comprehensive integration tests for REST controllers. Integration tests verify that the full request/response flow works correctly including:
- Request deserialization
- Validation
- Security/authentication
- Response serialization
- HTTP status codes
- Headers

## Current Test Coverage Gaps
Based on project structure, the following controller integration tests are missing or incomplete:

### Controllers Needing Tests
1. **CampaignController** (`/api/v1/campaigns`)
   - [ ] POST /api/v1/campaigns (create)
   - [ ] GET /api/v1/campaigns (list with pagination, sorting, filtering)
   - [ ] GET /api/v1/campaigns/{id} (find by id)
   - [ ] PUT /api/v1/campaigns/{id} (update)

2. **BusinessController** (`/api/v1/businesses`)
   - [ ] POST /api/v1/businesses (create)
   - [ ] GET /api/v1/businesses (list)
   - [ ] GET /api/v1/businesses/{id} (find by id)
   - [ ] GET /api/v1/businesses?userId={id} (find by user)
   - [ ] PUT /api/v1/businesses/{id} (update)
   - [ ] DELETE /api/v1/businesses/{id} (delete)

3. **ProductController** (`/api/v1/products`)
   - [ ] POST /api/v1/products (create)
   - [ ] POST /api/v1/products/link (link to business)
   - [ ] GET /api/v1/products?businessId={id} (list by business)

4. **UserController** (`/api/v1/users`)
   - [ ] POST /api/v1/users (register)
   - [ ] GET /api/v1/users/{id} (get profile)
   - [ ] PUT /api/v1/users/{id} (update profile)

5. **FavoriteController** (`/api/v1/favorites`)
   - [ ] POST /api/v1/favorites (create)
   - [ ] GET /api/v1/favorites (list with type filter)
   - [ ] DELETE /api/v1/favorites/{id} (delete)

6. **CategoryController** (`/api/v1/categories`)
   - [ ] GET /api/v1/categories/{id} (find)
   - [ ] GET /api/v1/categories?parentId={id} (find by parent)
   - [ ] GET /api/v1/categories (find all)
   - [ ] POST /api/v1/categories (create)

7. **UserRegisterController** (`/api/v1/user-register`)
   - [ ] POST /api/v1/user-register/confirm-code (confirm)

8. **TimezoneController** (`/api/v1/timezones`)
   - [ ] GET /api/v1/timezones (paginated)
   - [ ] GET /api/v1/timezones/list (all)

9. **DigitalContactController** (`/api/v1/digital-contacts`)
   - [ ] GET /api/v1/digital-contacts (paginated)
   - [ ] GET /api/v1/digital-contacts/{id} (find by id)
   - [ ] GET /api/v1/digital-contacts?businessId={id} (find by business)

## Test Approach
Use `@SpringBootTest` with `@AutoConfigureMockMvc` for integration testing:

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CampaignControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CampaignService campaignService;
    
    @Test
    void createCampaign_ValidRequest_ReturnsCreated() throws Exception {
        // Arrange
        CampaignTO request = new CampaignTO(...);
        when(campaignService.create(any())).thenReturn(ResponseEntity.status(201).body(request));
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
    }
}
```

## Acceptance Criteria
1. **Minimum 3 integration tests per controller** covering:
   - Happy path (successful request)
   - Validation failure (400 Bad Request)
   - Not found scenario (404)

2. **Test coverage includes:**
   - Request body deserialization
   - Response body serialization
   - HTTP status codes
   - JSON structure validation
   - Authentication headers (if applicable)
   - Error response format

3. **Tests are isolated:**
   - Use `@MockBean` for service layer
   - Use H2 in-memory database (test profile)
   - Clean state between tests

## Technical Tasks
- [ ] Create controller integration test base class (shared setup)
- [ ] Implement `CampaignControllerIntegrationTest` (4 tests)
- [ ] Implement `BusinessControllerIntegrationTest` (6 tests)
- [ ] Implement `ProductControllerIntegrationTest` (3 tests)
- [ ] Implement `UserControllerIntegrationTest` (3 tests)
- [ ] Implement `FavoriteControllerIntegrationTest` (3 tests)
- [ ] Implement `CategoryControllerIntegrationTest` (4 tests)
- [ ] Implement `UserRegisterControllerIntegrationTest` (1 test)
- [ ] Implement `TimezoneControllerIntegrationTest` (2 tests)
- [ ] Implement `DigitalContactControllerIntegrationTest` (3 tests)
- [ ] Configure test profile with H2 database
- [ ] Add ObjectMapper configuration for JSON serialization
- [ ] Run full test suite and verify all pass

## Testing Requirements
- Use `MockMvc` for HTTP request simulation
- Use `@MockBean` to mock service layer
- Use Jackson `ObjectMapper` for JSON serialization
- Assert HTTP status, headers, and response body
- Use `jsonPath()` for JSON assertions
- Follow existing test patterns in the codebase

## Dependencies
- Spring Boot Test (`@SpringBootTest`)
- MockMvc (`@AutoConfigureMockMvc`)
- H2 database (test profile)
- JUnit 5

## Estimated Effort
**16-20 hours**
- Base test class setup: 2 hours
- Campaign controller tests: 2 hours
- Business controller tests: 3 hours
- Product controller tests: 2 hours
- User controller tests: 2 hours
- Favorite controller tests: 2 hours
- Category controller tests: 2 hours
- Remaining controllers: 3 hours
- Debugging and fixes: 2 hours

## References
- Controllers: `src/main/java/com/prx/directory/api/v1/controller/*Controller.java`
- Test profile: `src/test/resources/application-test.yml`
- Example test pattern: Look for existing `*ControllerTest.java` if any

## Quality Gates
- [ ] All controller integration tests pass
- [ ] No PMD violations
- [ ] Tests run in CI pipeline
- [ ] Coverage report includes controller layer

## Business Value
Integration tests provide:
- **Confidence** that API contracts work end-to-end
- **Regression prevention** for API breaking changes
- **Documentation** of expected API behavior
- **Faster debugging** when issues occur

## Notes
This is a significant investment in test infrastructure but provides high value. Start with the most critical controllers (Campaign, Business, User) and expand coverage over time.

