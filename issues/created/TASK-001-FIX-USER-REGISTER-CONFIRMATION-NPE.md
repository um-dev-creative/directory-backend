# TASK-001: Fix NullPointerException in User Registration Confirmation Code

## Priority
**High**

## Status
Open

## Component
- user-register (service + controller)
- Package: `com.umdc.directory.api.v1.service`
- Class: `UserRegisterServiceImpl`

## Description
A NullPointerException occurs during the user registration confirmation flow when the confirmation code map does not contain the requested key. The server attempts to call `.stream()` on a null value returned by `Map.get(key)`, causing 500 errors.

## Current Behavior
- User submits a confirmation code via `POST /api/v1/user-register/confirm-code`
- If the code is not found in the internal map, `Map.get(key)` returns `null`
- Calling `.stream()` on null throws `NullPointerException`
- Response: HTTP 500 Internal Server Error

## Expected Behavior
- User submits a confirmation code
- If code is not found or expired, return structured error response
- Response: HTTP 404 Not Found or HTTP 422 Unprocessable Entity (based on business decision)
- Error body should follow consistent format:
```json
{
  "code": "CONFIRMATION_NOT_FOUND",
  "message": "Confirmation code not found or expired"
}
```

## Root Cause
`UserRegisterServiceImpl.confirmCode(...)` at line 59 calls `.stream()` directly on `map.get(key)` without null safety check.

## Acceptance Criteria
1. **Null-safe map access**
   - Use `Map.getOrDefault(key, Collections.emptyList())` or `Optional.ofNullable(map.get(key)).orElse(Collections.emptyList())`
   - No NullPointerException thrown when key is missing

2. **Structured error response**
   - Return HTTP 404 with error body containing `code` and `message` fields
   - Preserve `DirectoryAppConstants.MESSAGE_HEADER` semantics if applicable

3. **Success path unchanged**
   - Valid confirmation codes continue to work as before
   - No regression in happy path

4. **Test coverage**
   - Unit test for missing confirmation code scenario
   - Unit test for null map scenario
   - Unit test for valid confirmation code (existing behavior)

## Technical Tasks
- [ ] Add null-safe map access in `UserRegisterServiceImpl.confirmCode(...)`
- [ ] Return structured error response for missing codes
- [ ] Add/update unit tests in `UserRegisterServiceImplTest`
- [ ] Verify logging does not throw NPE
- [ ] Update API documentation if needed

## Testing Requirements
- Minimum 3 unit tests:
  1. Valid confirmation code returns success
  2. Missing confirmation code returns 404 with error body
  3. Null/empty map returns 404 with error body

## Dependencies
None

## Estimated Effort
**2-4 hours**

## References
- YouTrack Issue: `/youtrack/ISSUE-USER-REGISTER-CONFIRMATION-NPE.md`
- Service: `src/main/java/com/prx/directory/api/v1/service/UserRegisterServiceImpl.java`
- Test: `src/test/java/com/prx/directory/api/v1/service/UserRegisterServiceImplTest.java`

## Quality Gates
- [ ] Code coverage ≥ 80% line coverage for modified classes
- [ ] No PMD violations
- [ ] All tests pass
- [ ] No new SonarCloud issues

