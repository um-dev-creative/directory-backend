package com.prx.directory.api.v1.service;

import com.prx.commons.general.pojo.Application;
import com.prx.commons.general.pojo.Role;
import com.prx.directory.api.v1.to.*;
import com.prx.directory.client.backbone.BackboneClient;
import com.prx.directory.client.backbone.to.BackboneUserGetResponse;
import com.prx.directory.constant.ContactTypeKey;
import com.prx.directory.constant.RoleKey;
import com.prx.directory.jpa.entity.BusinessEntity;
import com.prx.directory.jpa.entity.ContactTypeEntity;
import com.prx.directory.jpa.entity.UserEntity;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.jpa.repository.CategoryRepository;
import com.prx.directory.jpa.repository.DigitalContactRepository;
import com.prx.directory.mapper.BusinessMapper;
import com.prx.directory.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("BusinessServiceImpl - unit tests for business flows")
class BusinessServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private BackboneClient backboneClient;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private DigitalContactRepository digitalContactRepository;

    @Mock
    private BusinessMapper businessMapper;

    @InjectMocks
    private BusinessServiceImpl businessService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(businessService, "initialRoleId", "3165ff6c-cdc3-4cbb-a19f-2252b15be6e2");
    }

    // --- create(...) tests ---

    @Test
    @DisplayName("create - success")
    void createBusiness_success() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Role role = new Role();
        BusinessCreateRequest req = new BusinessCreateRequest(
                "Example Business",
                "Description",
                categoryId,
                userId,
                "email@example.com",
                "support@example.com",
                "orders@example.com",
                "https://example.com");

        BusinessEntity entity = new BusinessEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(req.name());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastUpdate(LocalDateTime.now());

        role.setId(UUID.randomUUID());
        role.setName(RoleKey.LH_STANDARD.name());

        BackboneUserGetResponse backboneUserGetResponse = new BackboneUserGetResponse(
                userId,
                "alias",
                "12345GTGT",
                "user@email.com",
                "displayName",
                LocalDateTime.now(),
                LocalDateTime.now(),
                true,
                true,
                true,
                true,
                null,
                List.of(role),
                List.of(new Application())
        );

        // For a successful create, no existing business with same name
        when(businessRepository.findByName(req.name())).thenReturn(Optional.empty());
        when(backboneClient.findUserById(userId)).thenReturn(backboneUserGetResponse);
        when(businessMapper.toSource(req)).thenReturn(entity);
        when(businessRepository.save(entity)).thenReturn(entity);
        when(businessMapper.toBusinessCreateResponse(entity)).thenReturn(
                new BusinessCreateResponse(entity.getId(), entity.getName(), entity.getCreatedDate(), entity.getLastUpdate())
        );

        ResponseEntity<BusinessCreateResponse> response = businessService.create(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(entity.getId(), response.getBody().id());
    }

    @Test
    @DisplayName("create - null request -> BAD_REQUEST")
    void createBusiness_nullRequest_badRequest() {
        ResponseEntity<BusinessCreateResponse> response = businessService.create(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("create - duplicate name -> CONFLICT")
    void createBusiness_duplicateName_conflict() {
        BusinessCreateRequest req = new BusinessCreateRequest(
                "Example Business",
                "Description",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "email@example.com",
                null,
                null,
                null);

        when(businessRepository.findByName(req.name())).thenReturn(Optional.of(new BusinessEntity()));

        ResponseEntity<BusinessCreateResponse> response = businessService.create(req);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    @DisplayName("create - user not found in backbone -> BAD_REQUEST")
    void createBusiness_userNotFound_badRequest() {
        BusinessCreateRequest req = new BusinessCreateRequest(
                "Example Business",
                "Description",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "email@example.com",
                null,
                null,
                null);

        when(businessRepository.findByName(req.name())).thenReturn(Optional.empty());
        when(backboneClient.findUserById(req.userId())).thenReturn(null);

        ResponseEntity<BusinessCreateResponse> response = businessService.create(req);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("create - invalid category id -> BAD_REQUEST")
    void createBusiness_invalidCategory_badRequest() {
        UUID userId = UUID.randomUUID();
        BusinessCreateRequest req = new BusinessCreateRequest(
                "Example Business",
                "Description",
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                userId,
                "email@example.com",
                null,
                null,
                null);

        ResponseEntity<BusinessCreateResponse> response = businessService.create(req);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("create - invalid website -> BAD_REQUEST")
    void createBusiness_invalidWebsite_badRequest() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        BusinessCreateRequest req = new BusinessCreateRequest(
                "Example Business",
                "Description",
                categoryId,
                userId,
                "email@example.com",
                null,
                null,
                "not-a-url");

        ResponseEntity<BusinessCreateResponse> response = businessService.create(req);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // --- findById(...) tests ---

    @Test
    @DisplayName("findById - success")
    void findById_success() {
        UUID id = UUID.randomUUID();
        BusinessEntity entity = new BusinessEntity();
        entity.setId(id);

        BusinessTO to = new BusinessTO(id, "Name", "Desc", UUID.randomUUID(), UUID.randomUUID(),
                "email", null, null, null, LocalDateTime.now(), LocalDateTime.now(), false, UUID.randomUUID());

        when(businessRepository.findBusinessWithDigitalContactsById(id)).thenReturn(Optional.of(entity));
        when(businessMapper.toBusinessTO(entity)).thenReturn(to);

        ResponseEntity<BusinessTO> response = businessService.findById(id);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, Objects.requireNonNull(response.getBody()).id());
    }

    @Test
    @DisplayName("findById - not found")
    void findById_notFound() {
        UUID id = UUID.randomUUID();
        when(businessRepository.findBusinessWithDigitalContactsById(id)).thenReturn(Optional.empty());

        ResponseEntity<BusinessTO> response = businessService.findById(id);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // --- findByUserId(...) tests ---

    @Test
    @DisplayName("findByUserId - success with results")
    void findByUserId_success() {
        UUID userId = UUID.randomUUID();
        BusinessEntity entity = new BusinessEntity();
        entity.setId(UUID.randomUUID());
        Set<BusinessEntity> entities = Set.of(entity);

        BusinessTO to = new BusinessTO(entity.getId(), "Name", "Desc", userId, UUID.randomUUID(),
                "email", null, null, null, LocalDateTime.now(), LocalDateTime.now(), false, UUID.randomUUID());

        when(businessRepository.findByUserEntityFk(userId)).thenReturn(entities);
        when(businessMapper.toBusinessTO(entity)).thenReturn(to);

        ResponseEntity<Set<BusinessTO>> response = businessService.findByUserId(userId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(Objects.requireNonNull(response.getBody()).isEmpty());
    }

    @Test
    @DisplayName("findByUserId - empty result")
    void findByUserId_empty() {
        UUID userId = UUID.randomUUID();
        when(businessRepository.findByUserEntityFk(userId)).thenReturn(Collections.emptySet());

        ResponseEntity<Set<BusinessTO>> response = businessService.findByUserId(userId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(Objects.requireNonNull(response.getBody()).isEmpty());
    }

    // --- findByName(...) tests ---

    @Test
    @DisplayName("findByName - success")
    void findByName_success() {
        String name = "Example";
        BusinessEntity entity = new BusinessEntity();
        entity.setId(UUID.randomUUID());

        BusinessTO to = new BusinessTO(entity.getId(), name, "Desc", UUID.randomUUID(), UUID.randomUUID(),
                "email", null, null, null, LocalDateTime.now(), LocalDateTime.now(), false, UUID.randomUUID());

        when(businessRepository.findByName(name)).thenReturn(Optional.of(entity));
        when(businessMapper.toBusinessTO(entity)).thenReturn(to);

        ResponseEntity<BusinessTO> response = businessService.findByName(name);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(name, Objects.requireNonNull(response.getBody()).name());
    }

    @Test
    @DisplayName("findByName - blank name -> BAD_REQUEST")
    void findByName_blank_badRequest() {
        ResponseEntity<BusinessTO> response = businessService.findByName(" ");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("findByName - not found")
    void findByName_notFound() {
        String name = "Unknown";
        when(businessRepository.findByName(name)).thenReturn(Optional.empty());

        ResponseEntity<BusinessTO> response = businessService.findByName(name);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // --- update(...) tests (existing three extended a bit) ---

    @Test
    @DisplayName("update - success returns OK and update date set")
    void testUpdateBusinessSuccess() {
        UUID businessId = UUID.randomUUID();
        BusinessUpdateRequest request = new BusinessUpdateRequest(
                businessId,
                "Updated Name",
                "Updated Description",
                null,
                "contact@example.com",
                "support@example.com",
                "orders@example.com",
                "https://example.com");

        BusinessEntity existingBusiness = new BusinessEntity();
        existingBusiness.setId(businessId);

        // Stub business lookup
        when(businessRepository.findBusinessWithDigitalContactsById(businessId)).thenReturn(Optional.of(existingBusiness));
        // No duplicate name
        when(businessRepository.findByName(request.name())).thenReturn(Optional.empty());
        // Stub user lookup using request.id() (here businessId is reused as userId in service logic)
        BackboneUserGetResponse backboneUserGetResponse = new BackboneUserGetResponse(
                businessId,
                "alias",
                "12345GTGT",
                "user@email.com",
                "displayName",
                LocalDateTime.now(),
                LocalDateTime.now(),
                true,
                true,
                true,
                true,
                null,
                List.of(new Role()),
                List.of(new Application())
        );
        when(backboneClient.findUserById(businessId)).thenReturn(backboneUserGetResponse);
        // Save returns updated entity
        when(businessRepository.save(any(BusinessEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<BusinessUpdateResponse> response = businessService.update(businessId, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().updatedDate());
    }

    @Test
    @DisplayName("update - business not found returns NOT_FOUND")
    void testUpdateBusinessNotFound() {
        UUID businessId = UUID.randomUUID();
        BusinessUpdateRequest request = new BusinessUpdateRequest(
                businessId,
                "Updated Name",
                "Updated Description",
                null,
                "contact@example.com",
                "support@example.com",
                "orders@example.com",
                "https://example.com");

        when(businessRepository.findBusinessWithDigitalContactsById(businessId)).thenReturn(Optional.empty());

        ResponseEntity<BusinessUpdateResponse> response = businessService.update(businessId, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
    }

    @Test
    @DisplayName("update - validation failure returns BAD_REQUEST")
    void testUpdateBusinessValidationFailure() {
        ResponseEntity<BusinessUpdateResponse> response = businessService.update(UUID.randomUUID(), null);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
    }

    @Test
    @DisplayName("update - invalid website -> BAD_REQUEST")
    void update_invalidWebsite_badRequest() {
        UUID businessId = UUID.randomUUID();
        // Set id to null so that user validation is skipped and website is the first error
        BusinessUpdateRequest request = new BusinessUpdateRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "invalid-url");

        BusinessEntity existingBusiness = new BusinessEntity();
        existingBusiness.setId(businessId);
        when(businessRepository.findBusinessWithDigitalContactsById(businessId)).thenReturn(Optional.of(existingBusiness));

        ResponseEntity<BusinessUpdateResponse> response = businessService.update(businessId, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("update - name conflict -> CONFLICT")
    void update_nameConflict_conflict() {
        UUID businessId = UUID.randomUUID();
        BusinessUpdateRequest request = new BusinessUpdateRequest(
                businessId,
                "ExistingName",
                null,
                null,
                null,
                null,
                null,
                null);

        BusinessEntity existingBusiness = new BusinessEntity();
        existingBusiness.setId(businessId);

        // another business exists with the requested name and different id
        BusinessEntity other = new BusinessEntity();
        other.setId(UUID.randomUUID());

        when(businessRepository.findBusinessWithDigitalContactsById(businessId)).thenReturn(Optional.of(existingBusiness));
        when(businessRepository.findByName(request.name())).thenReturn(Optional.of(other));

        ResponseEntity<BusinessUpdateResponse> response = businessService.update(businessId, request);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    @DisplayName("update - category not found -> NOT_FOUND")
    void update_categoryNotFound_notFound() {
        UUID businessId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        BusinessUpdateRequest request = new BusinessUpdateRequest(
                null,
                null,
                null,
                categoryId,
                null,
                null,
                null,
                null);

        BusinessEntity existingBusiness = new BusinessEntity();
        existingBusiness.setId(businessId);

        when(businessRepository.findBusinessWithDigitalContactsById(businessId)).thenReturn(Optional.of(existingBusiness));
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        ResponseEntity<BusinessUpdateResponse> response = businessService.update(businessId, request);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("update - user lookup throws -> NOT_FOUND")
    void update_userLookupThrows_notFound() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BusinessUpdateRequest request = new BusinessUpdateRequest(
                userId,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        BusinessEntity existingBusiness = new BusinessEntity();
        existingBusiness.setId(businessId);

        when(businessRepository.findBusinessWithDigitalContactsById(businessId)).thenReturn(Optional.of(existingBusiness));
        when(backboneClient.findUserById(userId)).thenThrow(new RuntimeException("backend error"));

        ResponseEntity<BusinessUpdateResponse> response = businessService.update(businessId, request);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("update - create new digital contact when none exists")
    void update_createDigitalContact_whenNoneExists() {
        UUID businessId = UUID.randomUUID();
        BusinessUpdateRequest request = new BusinessUpdateRequest(
                null,
                null,
                null,
                null,
                "new@example.com",
                null,
                null,
                null);

        BusinessEntity existingBusiness = new BusinessEntity();
        existingBusiness.setId(businessId);
        existingBusiness.setDigitalContacts(null);

        when(businessRepository.findBusinessWithDigitalContactsById(businessId)).thenReturn(Optional.of(existingBusiness));
        when(businessRepository.save(any(BusinessEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<BusinessUpdateResponse> response = businessService.update(businessId, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // verify that save on digital contact repository was called for create
        verify(digitalContactRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("update - update existing digital contact when changed")
    void update_updateExistingDigitalContact_changed() {
        UUID businessId = UUID.randomUUID();
        BusinessUpdateRequest request = new BusinessUpdateRequest(
                null,
                null,
                null,
                null,
                "changed@example.com",
                null,
                null,
                null);

        BusinessEntity existingBusiness = new BusinessEntity();
        existingBusiness.setId(businessId);
        ContactTypeEntity ct = new ContactTypeEntity();
        ct.setName(ContactTypeKey.EML.toString());
        com.prx.directory.jpa.entity.DigitalContactEntity dc = new com.prx.directory.jpa.entity.DigitalContactEntity();
        dc.setContactType(ct);
        dc.setContent("old@example.com");
        Set<com.prx.directory.jpa.entity.DigitalContactEntity> contacts = new HashSet<>();
        contacts.add(dc);
        existingBusiness.setDigitalContacts(contacts);

        when(businessRepository.findBusinessWithDigitalContactsById(businessId)).thenReturn(Optional.of(existingBusiness));
        when(businessRepository.save(any(BusinessEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<BusinessUpdateResponse> response = businessService.update(businessId, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // verify that save on digital contact repository was called for update
        verify(digitalContactRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("update - existing digital contact same content -> no save")
    void update_existingDigitalContact_sameContent_noSave() {
        UUID businessId = UUID.randomUUID();
        BusinessUpdateRequest request = new BusinessUpdateRequest(
                null,
                null,
                null,
                null,
                "same@example.com",
                null,
                null,
                null);

        BusinessEntity existingBusiness = new BusinessEntity();
        existingBusiness.setId(businessId);
        ContactTypeEntity ct = new ContactTypeEntity();
        ct.setName(ContactTypeKey.EML.toString());
        com.prx.directory.jpa.entity.DigitalContactEntity dc = new com.prx.directory.jpa.entity.DigitalContactEntity();
        dc.setContactType(ct);
        dc.setContent("same@example.com");
        Set<com.prx.directory.jpa.entity.DigitalContactEntity> contacts = new HashSet<>();
        contacts.add(dc);
        existingBusiness.setDigitalContacts(contacts);

        when(businessRepository.findBusinessWithDigitalContactsById(businessId)).thenReturn(Optional.of(existingBusiness));
        when(businessRepository.save(any(BusinessEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<BusinessUpdateResponse> response = businessService.update(businessId, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // verify that save on digital contact repository was NOT called because content is same
        verify(digitalContactRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("deleteBusiness - success as owner")
    void deleteBusiness_successOwner() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        BusinessEntity entity = new BusinessEntity();
        entity.setId(businessId);
        UserEntity user = new UserEntity();
        user.setId(userId);
        entity.setUserFk(user);

        try (MockedStatic<JwtUtil> jwtMock = Mockito.mockStatic(JwtUtil.class)) {
            jwtMock.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(businessRepository.findById(businessId)).thenReturn(Optional.of(entity));
            when(businessRepository.countByUserId(userId)).thenReturn(1);

            ResponseEntity<Void> response = businessService.deleteBusiness(businessId, "token");
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Test
    @DisplayName("deleteBusiness - not owner -> FORBIDDEN")
    void deleteBusiness_notOwner_forbidden() {
        UUID businessId = UUID.randomUUID();
        UUID tokenUserId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        BusinessEntity entity = new BusinessEntity();
        entity.setId(businessId);
        UserEntity user = new UserEntity();
        user.setId(ownerId);
        entity.setUserFk(user);

        try (MockedStatic<JwtUtil> jwtMock = Mockito.mockStatic(JwtUtil.class)) {
            jwtMock.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(tokenUserId);
            when(businessRepository.findById(businessId)).thenReturn(Optional.of(entity));

            ResponseEntity<Void> response = businessService.deleteBusiness(businessId, "token");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Test
    @DisplayName("deleteBusiness - business not found -> NOT_FOUND")
    void deleteBusiness_notFound() {
        UUID businessId = UUID.randomUUID();

        try (MockedStatic<JwtUtil> jwtMock = Mockito.mockStatic(JwtUtil.class)) {
            jwtMock.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(UUID.randomUUID());
            when(businessRepository.findById(businessId)).thenReturn(Optional.empty());

            ResponseEntity<Void> response = businessService.deleteBusiness(businessId, "token");
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Test
    @DisplayName("deleteBusiness - invalid token -> UNAUTHORIZED")
    void deleteBusiness_invalidToken_unauthorized() {
        UUID businessId = UUID.randomUUID();

        try (MockedStatic<JwtUtil> jwtMock = Mockito.mockStatic(JwtUtil.class)) {
            jwtMock.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(null);

            ResponseEntity<Void> response = businessService.deleteBusiness(businessId, "token");
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }

    @Test
    @DisplayName("deleteBusiness - count zero triggers updateUserRole -> ACCEPTED")
    void deleteBusiness_countZero_triggersUpdateUserRole_accepted() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        BusinessEntity entity = new BusinessEntity();
        entity.setId(businessId);
        UserEntity user = new UserEntity();
        user.setId(userId);
        entity.setUserFk(user);

        try (MockedStatic<JwtUtil> jwtMock = Mockito.mockStatic(JwtUtil.class)) {
            jwtMock.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(businessRepository.findById(businessId)).thenReturn(Optional.of(entity));
            when(businessRepository.countByUserId(userId)).thenReturn(0);

            // Configure userService to provide a valid user and update to return OK
            GetUserResponse userTO = new GetUserResponse(userId, "alias", "email", null, null, null, "display", null, null, null, null, null, null, null,
                    true, true, true, true, UUID.randomUUID(), UUID.randomUUID());
            when(userService.findUser(anyString(), eq(userId))).thenReturn(ResponseEntity.ok(userTO));
            when(userService.update(eq(userId), any())).thenReturn(ResponseEntity.ok().build());

            ResponseEntity<Void> response = businessService.deleteBusiness(businessId, "token");
            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        }
    }

    @Test
    @DisplayName("deleteBusiness - count zero and findUser returns null -> FORBIDDEN")
    void deleteBusiness_countZero_updateUserRole_forbidden() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        BusinessEntity entity = new BusinessEntity();
        entity.setId(businessId);
        UserEntity user = new UserEntity();
        user.setId(userId);
        entity.setUserFk(user);

        try (MockedStatic<JwtUtil> jwtMock = Mockito.mockStatic(JwtUtil.class)) {
            jwtMock.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);
            when(businessRepository.findById(businessId)).thenReturn(Optional.of(entity));
            when(businessRepository.countByUserId(userId)).thenReturn(0);

            // findUser returns 200 but body null
            when(userService.findUser(anyString(), eq(userId))).thenReturn(ResponseEntity.ok(null));

            ResponseEntity<Void> response = businessService.deleteBusiness(businessId, "token");
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    // --- findIdByUserId(...) tests ---

    @Test
    @DisplayName("findIdByUserId - null id -> empty set")
    void findIdByUserId_null_returnsEmpty() {
        Set<UUID> result = businessService.findIdByUserId(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findIdByUserId - success")
    void findIdByUserId_success() {
        UUID userId = UUID.randomUUID();
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
        when(businessRepository.findIdCollectionByUserId(userId)).thenReturn(ids);

        Set<UUID> result = businessService.findIdByUserId(userId);
        assertEquals(ids, result);
    }
}
