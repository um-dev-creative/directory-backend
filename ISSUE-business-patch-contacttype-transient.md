# Bug: TransientPropertyValueException when patching Business -> DigitalContact.contactTypeEntity is transient

## Summary

Patching a Business (via the business PATCH endpoint) that includes or modifies `digitalContacts` fails with a Hibernate `TransientPropertyValueException`:

> Not-null property references a transient value - transient instance must be saved before current operation: com.umdc.entity.jpa.directory.DigitalContactEntity.contactTypeEntity -> com.umdc.entity.jpa.directory.ContactTypeEntity

This prevents updates to business digital contacts that reference contact types by nested payload (or when mapping creates new transient `ContactTypeEntity` instances). We must fix the mapping or service code to ensure `ContactTypeEntity` references are managed/persistent proxies before saving `DigitalContactEntity`.

## Affected area

Likely files to inspect/change (examples — adjust if package/file names differ):
- `src/main/java/com/prx/directory/jpa/entity/DigitalContactEntity.java`
- `src/main/java/com/prx/directory/jpa/entity/ContactTypeEntity.java`
- `src/main/java/com/prx/directory/api/v1/to/DigitalContactTO.java` (or similar)
- `src/main/java/com/prx/directory/mapper/*Mapper.java` (DigitalContact or Business mapper)
- `src/main/java/com/prx/directory/services/BusinessServiceImpl.java` (or relevant service that handles the patch)
- Tests: add/modify `src/test/java/.../BusinessServiceImplTest.java` and an integration test in `src/test/java/.../BusinessControllerIntegrationTest.java`

## Error excerpt

```
org.hibernate.TransientPropertyValueException:
 Not-null property references a transient value - transient instance must be saved before current operation:
 com.umdc.entity.jpa.directory.DigitalContactEntity.contactTypeEntity -> com.umdc.entity.jpa.directory.ContactTypeEntity
 ...
```

## Root cause (likely)

- The application code maps the incoming DTO to `DigitalContactEntity` and sets a `ContactTypeEntity` instance that is a new/transient object (e.g., created with `new ContactTypeEntity(id)` or mapped by MapStruct without resolving to a managed entity).
- On persist/merge, Hibernate expects the many-to-one `contactTypeEntity` to be managed (persisted) or annotated with cascade persist; because it's transient and non-nullable, Hibernate throws `TransientPropertyValueException`.
- Contact types are typically stable/reference data (predefined). Creating new transient `ContactTypeEntity` objects is incorrect. The reference should be a managed entity (loaded or proxied) or the mapping should set only the FK (id) and let JPA manage the association.

## Correct behavior

- When updating/patching a business with digital contacts that reference a contact type, the code should either:
  - Resolve the `ContactTypeEntity` to an existing managed entity (using `getReferenceById` or `findById`) and set that on `DigitalContactEntity`, or
  - If API accepts only `contactTypeId`, map to the entity reference properly.
- Do not persist new `ContactTypeEntity` instances as part of the business update (unless it's a deliberate create-contact-type workflow).

## Reproduction steps

1. Start application (test profile).
2. Send a PATCH or POST to the business update endpoint (example paths — adjust to actual endpoint, e.g. `/api/v1/businesses/{businessId}`).
3. Example payload that triggers the error (embedding a nested contactType object that the mapper turns into a new transient `ContactTypeEntity`):

```json
PATCH /api/v1/businesses/123
Content-Type: application/json

{
  "id": 123,
  "digitalContacts": [
    {
      "value": "+15551234567",
      "label": "Support",
      "contactType": {
        "id": 5,
        "name": "phone"
      }
    }
  ]
}
```

4. The server returns 500 with the `TransientPropertyValueException` stack trace.

## Recommended fixes (in order of preference)

Option A — Preferred: Map DTO contactType -> managed entity reference using `getReferenceById`

- In the service layer where DTOs are mapped to entities (or in mapper after-mapping), resolve the `ContactTypeEntity` reference using `JpaRepository.getReferenceById(id)` (Spring Data JPA) or `EntityManager.getReference(...)`. This returns a lazily-initialized managed proxy and avoids the transient error without extra DB hits (or with a single proxy load when required).
- Example change in service (pseudocode):

```java
// inject repository
private final ContactTypeRepository contactTypeRepository;

// inside patch/update method when mapping digital contacts:
if (dto.getContactType() != null && dto.getContactType().getId() != null) {
    Long typeId = dto.getContactType().getId();
    ContactTypeEntity typeRef = contactTypeRepository.getReferenceById(typeId);
    digitalContactEntity.setContactTypeEntity(typeRef);
} else {
    // handle missing id (validation error) or null contactType (clear association)
}
```

Option B — Map only the FK and let JPA set the association on merge

- If your mapping supports setting only `contactTypeId` on DTO, map that to a surrogate field or call `entityManager.getReference(...)` as above. Avoid creating a new `ContactTypeEntity` instance with only an id.

Option C — (Not recommended) Enable cascade persist on `@ManyToOne` to allow persisting new `ContactTypeEntity`

- Only valid if creating new contact types from this flow is desired (rare). Modifying cascade semantics is risky for reference data; prefer explicit creation flows.

## Minimal MapStruct approach

If MapStruct is being used, prefer an `@AfterMapping` method or `@Context` injection to convert `ContactTypeTO` into a reference:

```java
@Mapper(componentModel = "spring", uses = {ContactTypeMapper.class})
public interface DigitalContactMapper {

    @Mapping(target = "contactTypeEntity", ignore = true) // handled in afterMapping
    DigitalContactEntity toEntity(DigitalContactTO to, @Context ContactTypeResolver resolver);

    @AfterMapping
    default void resolveContactType(DigitalContactTO to, @MappingTarget DigitalContactEntity entity,
                                    @Context ContactTypeResolver resolver) {
        if (to.getContactType() != null && to.getContactType().getId() != null) {
            entity.setContactTypeEntity(resolver.getReference(to.getContactType().getId()));
        }
    }
}

@Component
public class ContactTypeResolver {
    private final ContactTypeRepository repo;
    public ContactTypeResolver(ContactTypeRepository repo) { this.repo = repo; }
    public ContactTypeEntity getReference(Long id) { return repo.getReferenceById(id); }
}
```

## Acceptance criteria (Given / When / Then)

- Scenario 1 — Successful patch that references existing contact type
  - Given an existing Business with id `123` and an existing ContactType with id `5`
  - When PATCH `/api/v1/businesses/123` is called with the digital contact referencing contact type id `5`
  - Then return 200 OK (or existing success semantics for patch) and the Business is updated; no `TransientPropertyValueException` thrown.
  - Example payload:
  ```json
  {
    "digitalContacts": [
      {
        "value": "+15551234567",
        "label": "Support",
        "contactType": {
          "id": 5
        }
      }
    ]
  }
  ```

- Scenario 2 — Missing or invalid contactType id
  - Given a PATCH referencing non-existing contactType id `9999`
  - When the request is processed
  - Then return 400 Bad Request (or 404 depending on service conventions) with message header `DirectoryAppConstants.MESSAGE_HEADER` and body message: `"Contact type not found for id 9999"` or similar existing error payload format.

- Scenario 3 — Null contactType (clear association)
  - Given a PATCH that sets `contactType` to `null` for a digital contact
  - When request is processed
  - Then the digital contact's contactTypeEntity is set to `null` (if allowed by DB) or appropriate validation error is returned if not nullable.

## Tests to add

- Unit tests for the mapper/service to assert that when a DTO with `contactType.id` is mapped, the service calls `contactTypeRepository.getReferenceById(id)` and sets it on the entity. Use Mockito to verify interaction and avoid persisting `ContactTypeEntity`.
- Integration test:
  - Setup H2 test DB with seeded `ContactTypeEntity` rows.
  - Perform PATCH to `/api/v1/businesses/{id}` and assert:
    - 200 OK
    - The `digitalContacts` entry has `contactType_id = 5` (db-level check)
    - No `TransientPropertyValueException` occurs.
- Negative tests for invalid id returning 400/404.

Example unit test skeleton:

```java
@Test
void patchBusiness_resolvesContactTypeReference() {
    ContactTypeRepository repo = mock(ContactTypeRepository.class);
    when(repo.getReferenceById(5L)).thenReturn(new ContactTypeEntity(5L));
    // map/patch...
    verify(repo).getReferenceById(5L);
    // assert entity.contactTypeEntity.getId() == 5
}
```

## Files to change (concrete recommendations)
- `src/main/java/com/prx/directory/services/BusinessServiceImpl.java` — update patch/update logic to resolve contact type references before saving.
- `src/main/java/com/prx/directory/mapper/*Mapper.java` — update MapStruct mapping or add `@AfterMapping` resolver if mapping is used.
- Add `ContactTypeResolver` or use existing `ContactTypeRepository` directly in service.
- Add unit + integration tests in `src/test/java/...`.
- Update logs/messages to use `DirectoryAppConstants.MESSAGE_HEADER` for client-facing errors.

## API contract / compatibility notes
- No API contract change is required if clients continue to send nested `contactType` with `id`. The server should accept the current format and handle it correctly. Keep `/api/v1/*` backward compatible.
- If you decide to change the API to require `contactTypeId` instead of nested object, then update:
  - `*Api.java` endpoint annotations
  - `src/main/resources/api/index.yaml`
But changing the contract is not required to fix this bug.

## PR checklist
- [ ] Implement code fix (prefer Option A)
- [ ] Add/modify unit tests and integration tests
- [ ] Run `mvn -DskipTests package` and `mvn test`
- [ ] Ensure JaCoCo coverage thresholds are met
- [ ] Add descriptive commit and branch name (e.g., `bug/business-patch-contacttype-transient`)
- [ ] CI green, then create PR linking this issue

## Suggested error-handling semantics
- When a provided `contactType.id` does not exist:
  - Return 400 Bad Request with message in header `DirectoryAppConstants.MESSAGE_HEADER` and body containing validation error.
- Avoid returning 500 for client-sent invalid ids.

## Example minimal fix diff (conceptual)
Add or modify code in the service where mapping occurs:

```java
// in BusinessServiceImpl (or mapper resolver)
if (to.getContactType() != null && to.getContactType().getId() != null) {
    Long typeId = to.getContactType().getId();
    ContactTypeEntity typeRef = contactTypeRepository.getReferenceById(typeId); // avoids transient exception
    digitalContactEntity.setContactTypeEntity(typeRef);
} else {
    digitalContactEntity.setContactTypeEntity(null);
}
```

Alternatively, if you prefer to check existence first:

```java
if (!contactTypeRepository.existsById(typeId)) {
    throw new BadRequestException("Contact type not found for id " + typeId);
}
ContactTypeEntity typeRef = contactTypeRepository.getReferenceById(typeId);
```

## Rollout & monitoring
- Deploy fix to QA environment and run the same patch that previously failed. Confirm logs no longer contain `TransientPropertyValueException`.
- Monitor logs for related errors for 24–48 hours after deployment.

## Estimated effort
- Investigation + code change + unit tests: 2–4 hours
- Integration tests + QA verification: 1–2 hours
Total: 3–6 hours (medium priority bug)

## Labels / Priority / Assignee
- Labels: bug, backend, jpa, spring-data, urgent
- Priority: High (blocking business updates)
- Assignee: backend team lead or developer familiar with `BusinessServiceImpl` and mapping (suggest: `@backend-dev`)


---

If you want, I can:
- Draft the actual code changes (service + MapStruct afterMapping) and unit tests.
- Run a quick grep to locate the exact mapper and service class names in the repo and tailor the snippets to the real file/class names.
Which would you prefer next?

