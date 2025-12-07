package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.*;
import com.prx.directory.client.backbone.BackboneClient;
import com.prx.directory.client.backbone.to.BackboneUserGetResponse;
import com.prx.directory.client.backbone.to.BackboneUserUpdateRequest;
import com.prx.directory.jpa.entity.BusinessEntity;
import com.prx.directory.jpa.entity.UserEntity;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.jpa.repository.CategoryRepository;
import com.prx.directory.jpa.repository.DigitalContactRepository;
import com.prx.directory.mapper.BusinessMapper;
import com.prx.directory.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessServiceImplExtendedTest {

    @Mock
     UserService userService;
    @Mock
     BusinessRepository businessRepository;
    @Mock
     BusinessMapper businessMapper;
    @Mock
    BackboneClient backboneClient;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    DigitalContactRepository digitalContactRepository;

    private BusinessServiceImpl service;
    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final String INITIAL_ROLE_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        service = new BusinessServiceImpl(userService, businessRepository,categoryRepository, digitalContactRepository, businessMapper, backboneClient);
        ReflectionTestUtils.setField(service, "applicationId", APPLICATION_ID);
        ReflectionTestUtils.setField(service, "initialRoleId", INITIAL_ROLE_ID);
    }

    @Test
    @DisplayName("create - success with valid request")
    void create_success() {
        BusinessCreateRequest request = new BusinessCreateRequest("Test Business", "Description",
                UUID.randomUUID(), UUID.randomUUID(), "test@example.com", "contact@example.com",
                "support@example.com", "https://example.com");
        BusinessEntity entity = new BusinessEntity();
        entity.setId(UUID.randomUUID());
        BusinessCreateResponse expected = new BusinessCreateResponse(entity.getId(),
                "Test Business", LocalDateTime.now(), LocalDateTime.now());
        BackboneUserGetResponse userResponse = mock(BackboneUserGetResponse.class);
        when(userResponse.roles()).thenReturn(List.of());

        when(businessRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(backboneClient.findUserById(any(UUID.class))).thenReturn(userResponse);
        when(businessMapper.toSource(request)).thenReturn(entity);
        when(businessRepository.save(entity)).thenReturn(entity);
        when(businessMapper.toBusinessCreateResponse(entity)).thenReturn(expected);

        ResponseEntity<BusinessCreateResponse> result = service.create(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        verify(businessRepository).save(entity);
    }

    @Test
    @DisplayName("create - null request returns BAD_REQUEST")
    void create_nullRequest() {
        ResponseEntity<BusinessCreateResponse> result = service.create(null);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
        verifyNoInteractions(businessRepository);
    }

    @Test
    @DisplayName("create - null categoryId returns BAD_REQUEST")
    void create_nullCategoryId() {
        BusinessCreateRequest request = new BusinessCreateRequest("Test Business", "Description",
                null, UUID.randomUUID(), "test@example.com", "contact@example.com",
                "support@example.com", "https://example.com");

        ResponseEntity<BusinessCreateResponse> result = service.create(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("create - empty categoryId returns BAD_REQUEST")
    void create_emptyCategoryId() {
        BusinessCreateRequest request = new BusinessCreateRequest("Test Business", "Description",
                UUID.fromString("00000000-0000-0000-0000-000000000000"), UUID.randomUUID(),
                "test@example.com", "contact@example.com", "support@example.com", "https://example.com");

        ResponseEntity<BusinessCreateResponse> result = service.create(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("create - null userId returns BAD_REQUEST")
    void create_nullUserId() {
        BusinessCreateRequest request = new BusinessCreateRequest("Test Business", "Description",
                UUID.randomUUID(), null, "test@example.com", "contact@example.com",
                "support@example.com", "https://example.com");

        ResponseEntity<BusinessCreateResponse> result = service.create(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("create - empty userId returns BAD_REQUEST")
    void create_emptyUserId() {
        BusinessCreateRequest request = new BusinessCreateRequest("Test Business", "Description",
                UUID.randomUUID(), UUID.fromString("00000000-0000-0000-0000-000000000000"),
                "test@example.com", "contact@example.com", "support@example.com", "https://example.com");

        ResponseEntity<BusinessCreateResponse> result = service.create(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("create - invalid website URL returns BAD_REQUEST")
    void create_invalidWebsite() {
        BusinessCreateRequest request = new BusinessCreateRequest("Test Business", "Description",
                UUID.randomUUID(), UUID.randomUUID(), "test@example.com", "contact@example.com",
                "support@example.com", "not-a-valid-url");

        ResponseEntity<BusinessCreateResponse> result = service.create(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("create - duplicate business name returns CONFLICT")
    void create_duplicateName() {
        BusinessCreateRequest request = new BusinessCreateRequest("Existing Business", "Description",
                UUID.randomUUID(), UUID.randomUUID(), "test@example.com", "contact@example.com",
                "support@example.com", "https://example.com");

        when(businessRepository.findByName("Existing Business")).thenReturn(Optional.of(new BusinessEntity()));

        ResponseEntity<BusinessCreateResponse> result = service.create(request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("create - user not found returns BAD_REQUEST")
    void create_userNotFound() {
        BusinessCreateRequest request = new BusinessCreateRequest("Test Business", "Description",
                UUID.randomUUID(), UUID.randomUUID(), "test@example.com", "contact@example.com",
                "support@example.com", "https://example.com");

        when(businessRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(backboneClient.findUserById(any(UUID.class))).thenReturn(null);

        ResponseEntity<BusinessCreateResponse> result = service.create(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("create - RuntimeException returns CONFLICT")
    void create_runtimeException() {
        BusinessCreateRequest request = new BusinessCreateRequest("Test Business", "Description",
                UUID.randomUUID(), UUID.randomUUID(), "test@example.com", "contact@example.com",
                "support@example.com", "https://example.com");
        BackboneUserGetResponse userResponse = mock(BackboneUserGetResponse.class);

        when(businessRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(backboneClient.findUserById(any(UUID.class))).thenReturn(userResponse);
        when(businessMapper.toSource(request)).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<BusinessCreateResponse> result = service.create(request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("create - updates user role when LH_STANDARD role present")
    void create_updatesRoleForStandardUser() {
        BusinessCreateRequest request = new BusinessCreateRequest("Test Business", "Description",
                UUID.randomUUID(), UUID.randomUUID(), "test@example.com", "contact@example.com",
                "support@example.com", "https://example.com");
        BusinessEntity entity = new BusinessEntity();
        entity.setId(UUID.randomUUID());
        BusinessCreateResponse expected = new BusinessCreateResponse(entity.getId(),
                "Test Business", LocalDateTime.now(), LocalDateTime.now());

        var role = mock(com.prx.commons.general.pojo.Role.class);
        when(role.getName()).thenReturn("LH_STANDARD");
        BackboneUserGetResponse userResponse = mock(BackboneUserGetResponse.class);
        when(userResponse.id()).thenReturn(UUID.randomUUID());
        when(userResponse.roles()).thenReturn(List.of(role));
        when(userResponse.notificationEmail()).thenReturn(true);
        when(userResponse.notificationSms()).thenReturn(false);
        when(userResponse.privacyDataOutActive()).thenReturn(true);

        when(businessRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(backboneClient.findUserById(any(UUID.class))).thenReturn(userResponse);
        when(businessMapper.toSource(request)).thenReturn(entity);
        when(businessRepository.save(entity)).thenReturn(entity);
        when(businessMapper.toBusinessCreateResponse(entity)).thenReturn(expected);
        when(backboneClient.userPartialUpdate(any(UUID.class), any(BackboneUserUpdateRequest.class)))
                .thenReturn(ResponseEntity.ok().build());

        ResponseEntity<BusinessCreateResponse> result = service.create(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        verify(backboneClient).userPartialUpdate(any(UUID.class), any(BackboneUserUpdateRequest.class));
    }

    @Test
    @DisplayName("findById - success returns OK with business")
    void findById_success() {
        UUID businessId = UUID.randomUUID();
        BusinessEntity entity = new BusinessEntity();
        entity.setId(businessId);
        BusinessTO expected = mock(BusinessTO.class);

        when(businessRepository.findBusinessWithDigitalContactsById(businessId)).thenReturn(Optional.of(entity));
        when(businessMapper.toBusinessTO(entity)).thenReturn(expected);

        ResponseEntity<BusinessTO> result = service.findById(businessId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(expected, result.getBody());
    }

    @Test
    @DisplayName("findById - not found returns NOT_FOUND")
    void findById_notFound() {
        UUID businessId = UUID.randomUUID();

        when(businessRepository.findBusinessWithDigitalContactsById(businessId)).thenReturn(Optional.empty());

        ResponseEntity<BusinessTO> result = service.findById(businessId);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    @DisplayName("findByUserId - success returns businesses")
    void findByUserId_success() {
        UUID userId = UUID.randomUUID();
        BusinessEntity entity1 = new BusinessEntity();
        BusinessEntity entity2 = new BusinessEntity();
        BusinessTO to1 = mock(BusinessTO.class);
        BusinessTO to2 = mock(BusinessTO.class);

        when(businessRepository.findByUserEntityFk(userId)).thenReturn(Set.of(entity1, entity2));
        when(businessMapper.toBusinessTO(entity1)).thenReturn(to1);
        when(businessMapper.toBusinessTO(entity2)).thenReturn(to2);

        ResponseEntity<Set<BusinessTO>> result = service.findByUserId(userId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().size());
    }

    @Test
    @DisplayName("findByUserId - empty result returns OK with empty set")
    void findByUserId_empty() {
        UUID userId = UUID.randomUUID();

        when(businessRepository.findByUserEntityFk(userId)).thenReturn(Set.of());

        ResponseEntity<Set<BusinessTO>> result = service.findByUserId(userId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    @DisplayName("findByName - success returns OK")
    void findByName_success() {
        String name = "Test Business";
        BusinessEntity entity = new BusinessEntity();
        BusinessTO expected = mock(BusinessTO.class);

        when(businessRepository.findByName(name)).thenReturn(Optional.of(entity));
        when(businessMapper.toBusinessTO(entity)).thenReturn(expected);

        ResponseEntity<BusinessTO> result = service.findByName(name);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(expected, result.getBody());
    }

    @Test
    @DisplayName("findByName - null name returns BAD_REQUEST")
    void findByName_nullName() {
        ResponseEntity<BusinessTO> result = service.findByName(null);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("findByName - blank name returns BAD_REQUEST")
    void findByName_blankName() {
        ResponseEntity<BusinessTO> result = service.findByName("   ");

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("findByName - not found returns NOT_FOUND")
    void findByName_notFound() {
        String name = "Nonexistent Business";

        when(businessRepository.findByName(name)).thenReturn(Optional.empty());

        ResponseEntity<BusinessTO> result = service.findByName(name);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    @DisplayName("deleteBusiness - success returns OK")
    void deleteBusiness_success() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String token = "valid-token";
        BusinessEntity business = new BusinessEntity();
        UserEntity user = new UserEntity();
        user.setId(userId);
        business.setUserFk(user);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(token)).thenReturn(userId);
            when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
            when(businessRepository.countByUserId(userId)).thenReturn(1);

            ResponseEntity<Void> result = service.deleteBusiness(businessId, token);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            verify(businessRepository).deleteById(businessId);
        }
    }

    @Test
    @DisplayName("deleteBusiness - null token userId returns UNAUTHORIZED")
    void deleteBusiness_nullUserId() {
        UUID businessId = UUID.randomUUID();
        String token = "invalid-token";

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(token)).thenReturn(null);

            ResponseEntity<Void> result = service.deleteBusiness(businessId, token);

            assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
            verifyNoInteractions(businessRepository);
        }
    }

    @Test
    @DisplayName("deleteBusiness - business not found returns NOT_FOUND")
    void deleteBusiness_notFound() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String token = "valid-token";

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(token)).thenReturn(userId);
            when(businessRepository.findById(businessId)).thenReturn(Optional.empty());

            ResponseEntity<Void> result = service.deleteBusiness(businessId, token);

            assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        }
    }

    @Test
    @DisplayName("deleteBusiness - not owner returns FORBIDDEN")
    void deleteBusiness_notOwner() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        String token = "valid-token";
        BusinessEntity business = new BusinessEntity();
        UserEntity user = new UserEntity();
        user.setId(otherUserId);
        business.setUserFk(user);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(token)).thenReturn(userId);
            when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));

            ResponseEntity<Void> result = service.deleteBusiness(businessId, token);

            assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
            verify(businessRepository, never()).deleteById(any());
        }
    }

    @Test
    @DisplayName("deleteBusiness - last business updates user role")
    void deleteBusiness_lastBusinessUpdatesRole() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String token = "valid-token";
        BusinessEntity business = new BusinessEntity();
        UserEntity user = new UserEntity();
        user.setId(userId);
        business.setUserFk(user);
        GetUserResponse userResp = mock(GetUserResponse.class);
        when(userResp.notificationEmail()).thenReturn(true);
        when(userResp.notificationSms()).thenReturn(true);
        when(userResp.privacyDataOutActive()).thenReturn(true);
        when(userResp.status()).thenReturn(true);

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(token)).thenReturn(userId);
            when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
            when(businessRepository.countByUserId(userId)).thenReturn(0);
            when(userService.findUser(token, userId)).thenReturn(ResponseEntity.ok(userResp));
            when(userService.update(eq(userId), any(PutUserRequest.class)))
                    .thenReturn(ResponseEntity.ok().build());

            ResponseEntity<Void> result = service.deleteBusiness(businessId, token);

            assertEquals(HttpStatus.ACCEPTED, result.getStatusCode());
            verify(userService).update(eq(userId), any(PutUserRequest.class));
        }
    }

    @Test
    @DisplayName("deleteBusiness - exception returns INTERNAL_SERVER_ERROR")
    void deleteBusiness_exception() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String token = "valid-token";

        try (MockedStatic<JwtUtil> mockedStatic = mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(token)).thenReturn(userId);
            when(businessRepository.findById(businessId)).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<Void> result = service.deleteBusiness(businessId, token);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        }
    }

    @Test
    @DisplayName("findIdByUserId - null id returns empty set")
    void findIdByUserId_nullId() {
        Set<UUID> result = service.findIdByUserId(null);

        assertTrue(result.isEmpty());
        verifyNoInteractions(businessRepository);
    }

    @Test
    @DisplayName("findIdByUserId - success returns UUIDs")
    void findIdByUserId_success() {
        UUID userId = UUID.randomUUID();
        Set<UUID> expected = Set.of(UUID.randomUUID(), UUID.randomUUID());

        when(businessRepository.findIdCollectionByUserId(userId)).thenReturn(expected);

        Set<UUID> result = service.findIdByUserId(userId);

        assertEquals(expected, result);
    }
}
