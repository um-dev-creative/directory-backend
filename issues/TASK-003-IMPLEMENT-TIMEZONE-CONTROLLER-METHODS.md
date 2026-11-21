# TASK-003: Implement Timezone Controller Default Methods

## Priority
**Low**

## Status
Open

## Component
- Timezone API
- Package: `com.umdc.directory.api.v1.controller`
- Class: `TimezoneApi` (interface)

## Description
The `TimezoneApi` interface has two default methods that throw `UnsupportedOperationException`. While `TimezoneServiceImpl` is fully implemented, the controller interface still has incomplete default methods preventing their use if not overridden.

## Current Behavior
- `TimezoneApi.getTimezones()` throws `UnsupportedOperationException("Not supported yet.")`
- `TimezoneApi.timezoneList()` throws `UnsupportedOperationException("Not implemented")`
- These methods are likely overridden in `TimezoneController`, but default implementations should be meaningful or removed

## Expected Behavior
Option 1: Remove the default implementations and make methods abstract
Option 2: Provide meaningful default implementations that delegate to service layer
Option 3: Document that these must be overridden by controller implementations

## Acceptance Criteria
1. **Decision on approach**
   - Review if default implementations are needed
   - If not needed, remove `default` keyword (make abstract)
   - If needed, implement proper delegation to service

2. **Code cleanup**
   - Remove `UnsupportedOperationException` throws
   - Ensure controller properly overrides methods

3. **Documentation**
   - Add JavaDoc explaining interface contract
   - Document expected behavior in API spec

## Technical Tasks
- [ ] Review `TimezoneApi` interface design
- [ ] Check if `TimezoneController` properly overrides methods
- [ ] Remove default implementations or implement properly
- [ ] Add/update JavaDoc
- [ ] Verify unit tests cover controller methods

## Testing Requirements
- Verify existing tests for `TimezoneController` pass
- Add integration test if controller methods were previously untested

## Dependencies
- `TimezoneService` (already implemented)
- `TimezoneController` (likely exists)

## Estimated Effort
**1-2 hours**

## References
- API Interface: `src/main/java/com/prx/directory/api/v1/controller/TimezoneApi.java`
- Service Implementation: `src/main/java/com/prx/directory/api/v1/service/TimezoneServiceImpl.java`

## Quality Gates
- [ ] No PMD violations
- [ ] All tests pass
- [ ] Code is clean and maintainable

## Notes
This is a low-priority technical debt item. The functionality may already work if the controller properly overrides these methods, but the default implementations should not throw exceptions.

