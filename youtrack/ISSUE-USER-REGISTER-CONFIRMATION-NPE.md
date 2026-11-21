# YouTrack Issue: Fix NPE in user-register confirmCode and add tests for user details without image

- Project: directory-backend
- Component: user-register (service + controller)
- Priority: High
- Reported date: 2026-03-23
- Reporter: (please fill)
- Assignee: (please assign)

## Summary
A NullPointerException occurs during the user registration confirmation flow. The server attempts to call `.stream()` on a collection returned by `Map.get(key)` without checking for null. This causes 500 responses and stack traces as shown below. Additionally, there is no explicit unit test ensuring that user details responses handle missing image/avatar correctly (no NPE and consistent JSON output).

## Relevant logs / stacktrace
```
2026-03-23T06:48:42.276Z  INFO 1 --- [directory-backend] [nio-8197-exec-5] c.p.c.s.loggers.LoggingServiceImp        : logResponse: RESPONSE method = [POST] responseBody = [null]
2026-03-23T07:00:51.929Z ERROR 1 --- [directory-backend] [nio-8197-exec-5] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: java.lang.NullPointerException: Cannot invoke "java.util.Collection.stream()" because the return value of "java.util.Map.get(Object)" is null] with root cause
java.lang.NullPointerException: Cannot invoke "java.util.Collection.stream()" because the return value of "java.util.Map.get(Object)" is null
	at com.umdc.service.v1.api.directory.UserRegisterServiceImpl.confirmCode(UserRegisterServiceImpl.java:59) ~[!/:0.0.1]
	at com.umdc.controller.v1.api.directory.UserRegisterController.confirmCode(UserRegisterController.java:21) ~[!/:0.0.1]
```

## Root cause
`UserRegisterServiceImpl.confirmCode(...)` calls `.stream()` directly on the result of `map.get(key)` without a null check. If the key is not present, `Map.get` returns `null` and `.stream()` triggers a NullPointerException.

## Suggested fix
- Make map access null-safe by using `Map.getOrDefault(key, Collections.emptyList())` or:
  - `Collection<Type> items = Optional.ofNullable(map.get(key)).orElse(Collections.emptyList());`
- Avoid calling `.stream()` on a potentially null reference.
- Ensure the service returns a controlled, documented HTTP response for missing/expired confirmation codes (e.g., 404 Not Found or 422 Unprocessable Entity depending on domain decision) instead of throwing.
- Ensure `LoggingServiceImp` logResponse handles null response bodies safely (it already logs `responseBody = [null]` in logs above, but validate no NPE in logging path).

Prefer a small, well-tested change rather than broad refactor. If testability is improved by constructor injection of the confirmation map, consider that refactor and include unit tests.

## Acceptance criteria (testable)
1. Null-safety for confirm flow
   - Given the confirmation map does not contain the requested key
   - When the client posts to the confirmation endpoint
   - Then the service must NOT throw NullPointerException
   - And must return a controlled error response (example below). Recommended: HTTP 404 with structured error body:

```json
{
  "code": "CONFIRMATION_NOT_FOUND",
  "message": "Confirmation code not found or expired"
}
```

   - The response must preserve `DirectoryAppConstants.MESSAGE_HEADER` semantics (if applicable in current API) and must be logged safely.

2. Success path unchanged
   - Given the confirmation map contains a matching code
   - When valid confirmation payload is posted
   - Then the service returns HTTP 200 OK (or 202 Accepted if current domain behavior) with the expected body (user DTO or token) per current contract.

3. User details without image
   - When requesting user details for a user with no avatar/image
   - The endpoint must return HTTP 200 OK with the user payload and `image` either `null` or omitted consistent with the API contract.
   - No NPE or server error should occur when `image` is absent in underlying persistence/model.

4. Tests and coverage
   - Add/modify unit tests to cover:
     - confirmCode when `map.get(key)` returns `null` -> assert no NPE and controlled error response
     - confirmCode when returned collection is empty -> assert controlled error response
     - confirmCode when code matches -> assert success response
     - getUserDetails for user without image -> assert 200 and `image` null/absent
   - Tests must be JUnit5 + Mockito consistent with project conventions
   - Maintain or increase overall coverage. Current project baseline: 80% line coverage at the bundle level. The change must not reduce coverage; aim to increase by at least 1% if feasible.

## Reproduction steps
1. Start application in test/dev environment (or run unit tests that exercise confirm code path).
2. POST to the confirm endpoint with a phone/email key not present in the service confirmation map.

Example (adjust path to real endpoint):

```bash
curl -X POST "http://localhost:8197/api/v1/user/register/confirm" \
  -H "Content-Type: application/json" \
  -d '{"phone":"missing-phone","code":"000000"}' -v
```

Expected: a 4xx controlled error response, not a 500 with NPE stacktrace.

## Unit test templates (JUnit5 + Mockito)

Below are recommended tests to add or update. Replace placeholder classes and fields with actual project types.

### Service test: confirmCode - null map.get -> not found

```java
package com.umdc.directory.api.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserRegisterServiceImplTest {

    @InjectMocks
    private UserRegisterServiceImpl service;

    // mock any dependencies used by the service
    @Mock
    private SomeDependency someDependency;

    @BeforeEach
    void setUp() {
        // Create a mock map that returns null for a missing key
        java.util.Map<String, java.util.Collection<String>> confirmationMap = mock(java.util.Map.class);
        when(confirmationMap.get("missing-phone")).thenReturn(null);
        // Inject into private field (replace "confirmationMap" with actual field name)
        ReflectionTestUtils.setField(service, "confirmationMap", confirmationMap);
    }

    @Test
    void confirmCode_whenMapGetReturnsNull_returnsNotFound() {
        ConfirmRequest req = new ConfirmRequest();
        req.setPhone("missing-phone");
        req.setCode("000000");

        ResponseEntity<?> response = service.confirmCode(req);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        // prefer explicit check if contract defines 404
        assertThat(response.getStatusCodeValue()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
    }
}
```

### Service test: confirmCode - empty collection -> not found

```java
@Test
void confirmCode_whenCollectionIsEmpty_returnsNotFound() {
    java.util.Map<String, java.util.Collection<String>> confirmationMap = mock(java.util.Map.class);
    when(confirmationMap.get("empty-phone")).thenReturn(Collections.emptyList());
    ReflectionTestUtils.setField(service, "confirmationMap", confirmationMap);

    ConfirmRequest req = new ConfirmRequest();
    req.setPhone("empty-phone");
    req.setCode("111111");

    ResponseEntity<?> response = service.confirmCode(req);

    assertThat(response.getStatusCodeValue()).isEqualTo(404);
}
```

### Service test: confirmCode - success path

```java
@Test
void confirmCode_whenCodeMatches_returnsOk() {
    java.util.List<String> codes = java.util.List.of("123456");
    java.util.Map<String, java.util.Collection<String>> confirmationMap = mock(java.util.Map.class);
    when(confirmationMap.get("phone-1")).thenReturn(codes);
    ReflectionTestUtils.setField(service, "confirmationMap", confirmationMap);

    ConfirmRequest req = new ConfirmRequest();
    req.setPhone("phone-1");
    req.setCode("123456");

    ResponseEntity<?> response = service.confirmCode(req);

    assertThat(response.getStatusCodeValue()).isEqualTo(200);
    // further assertions depending on contract
}
```

### Controller test: get user details when image is missing

```java
package com.umdc.directory.api.v1.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserRegisterController.class)
class UserRegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRegisterService userRegisterService;

    @Test
    void getUserDetails_withoutImage_returnsUserWithNullImage() throws Exception {
        UserDto user = new UserDto();
        user.setId("123");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane.doe@example.com");
        user.setImage(null);

        when(userRegisterService.getUser("123")).thenReturn(ResponseEntity.ok(user));

        mockMvc.perform(get("/api/v1/users/123").header("Authorization", "Bearer token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("123"))
            .andExpect(jsonPath("$.image").doesNotExist().or(jsonPath("$.image").value((Object) null)));
    }
}
```

Notes:
- Replace placeholder classes like `ConfirmRequest`, `UserDto`, `SomeDependency`, and field names with the project's actual classes.
- Prefer constructor injection of dependencies when refactoring for testability rather than direct field reflection. If refactoring is made, update tests accordingly.

## Run & test instructions
- Run all unit tests locally:

```bash
mvn test
```

- Run only the user-register service tests:

```bash
mvn -Dtest=com.umdc.service.v1.api.directory.UserRegisterServiceImplTest test
```

- CI full check:

```bash
mvn -B -V -e clean verify
```

## Definition of Done
- [ ] `UserRegisterServiceImpl.confirmCode(...)` updated to be null-safe and not call `.stream()` on `map.get(...)` without fallback
- [ ] Controlled error response for missing/expired confirmation codes implemented and documented
- [ ] Unit tests added/updated for the scenarios above and passing
- [ ] No NPE thrown for the reproduce case
- [ ] Coverage maintained or increased (bundle-level target >= current baseline; prefer +1%)
- [ ] If API contract changed, `*Api.java` annotations and `src/main/resources/api/index.yaml` updated and reviewed
- [ ] PR includes explanation, coverage report, and test results

## Additional context / risks
- If the confirmation map is populated asynchronously from another service or cache, consider race conditions or expiration windows.
- If this area of code is used by other flows, audit usages of the confirmation map for similar null-safety issues.

---

Please review and assign. I can also open a PR with the minimal code change and the unit tests if you want me to implement the fix and test updates.

