package com.umdev.directory.api.v1.service;

import com.umdev.commons.exception.StandardException;
import com.umdev.directory.api.v1.to.GetUserResponse;
import com.umdev.directory.api.v1.to.PutUserRequest;
import com.umdev.directory.api.v1.to.UserCreateRequest;
import com.umdev.directory.api.v1.to.UserCreateResponse;
import com.umdev.directory.client.backbone.BackboneClient;
import com.umdev.directory.client.backbone.to.BackboneUserCreateRequest;
import com.umdev.directory.client.backbone.to.BackboneUserCreateResponse;
import com.umdev.directory.client.backbone.to.BackboneUserGetResponse;
import com.umdev.directory.client.backbone.to.BackboneProfileImageRefResponse;
import com.umdev.directory.jpa.repository.BusinessRepository;
import com.umdev.directory.kafka.producer.EmailMessageProducerService;
import com.umdev.directory.mapper.GetUserMapper;
import com.umdev.directory.mapper.PutUserMapper;
import com.umdev.directory.mapper.UserCreateMapper;
import com.umdev.directory.client.backbone.to.BackboneUserUpdateRequest;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplExtendedTest {

    @Mock
    private BackboneClient backboneClient;
    @Mock
    private EmailMessageProducerService emailMessageProducerService;
    @Mock
    private UserCreateMapper userCreateMapper;
    @Mock
    private PutUserMapper putUserMapper;
    @Mock
    private GetUserMapper getUserMapper;
    @Mock
    private BusinessRepository businessRepository;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(backboneClient, emailMessageProducerService,
                userCreateMapper, putUserMapper, getUserMapper, businessRepository);
        ReflectionTestUtils.setField(service, "verificationCodeTemplateId", UUID.randomUUID());
        ReflectionTestUtils.setField(service, "applicationIdString", UUID.randomUUID().toString());
        ReflectionTestUtils.setField(service, "initialRoleId", UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("create - success case with valid request")
    void create_success() {
        UserCreateRequest request = new UserCreateRequest("password123", "john.doe@example.com",
                "John", "Doe", LocalDate.of(1990, 1, 1), "+1234567890",
                "JDoe", true, true, true);
        BackboneUserCreateResponse backboneResp = new BackboneUserCreateResponse(
                UUID.randomUUID(), "jdoe", "john.doe@example.com", LocalDateTime.now(),
                LocalDateTime.now(), true, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "JDoe", true, true, true);
        UserCreateResponse expected = new UserCreateResponse(backboneResp.id(),
                backboneResp.alias(), backboneResp.email(), backboneResp.createdDate(),
                backboneResp.lastUpdate(), backboneResp.active(), backboneResp.personId(),
                backboneResp.roleId(), backboneResp.applicationId(), backboneResp.displayName(),
                backboneResp.notificationSms(), backboneResp.notificationEmail(), backboneResp.privacyDataOutActive());

        when(backboneClient.checkEmail(anyString(), any(UUID.class))).thenReturn(ResponseEntity.ok().build());
        when(userCreateMapper.toBackbone(any(), any(), any(), anyString())).thenReturn(mock(BackboneUserCreateRequest.class));
        when(backboneClient.post(any())).thenReturn(backboneResp);
        when(userCreateMapper.fromBackbone(any())).thenReturn(expected);
        doNothing().when(emailMessageProducerService).sendMessage(any());

        ResponseEntity<UserCreateResponse> result = service.create(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(expected.id(), result.getBody().id());
        verify(emailMessageProducerService, times(1)).sendMessage(any());
    }

    @Test
    @DisplayName("create - null request returns BAD_REQUEST")
    void create_nullRequest() {
        ResponseEntity<UserCreateResponse> result = service.create(null);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertTrue(result.getHeaders().containsHeader("message-error"));
        verifyNoInteractions(backboneClient, emailMessageProducerService);
    }

    @Test
    @DisplayName("create - email already exists returns CONFLICT")
    void create_emailExists() {
        UserCreateRequest request = new UserCreateRequest("password123", "existing@example.com",
                "John", "Doe", LocalDate.of(1990, 1, 1), "+1234567890",
                "JDoe", true, true, true);

        when(backboneClient.checkEmail(anyString(), any(UUID.class)))
                .thenThrow(feignException(409, "Conflict"));

        ResponseEntity<UserCreateResponse> result = service.create(request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message-error"));
    }

    @Test
    @DisplayName("create - StandardException returns CONFLICT")
    void create_standardException() {
        UserCreateRequest request = new UserCreateRequest("password123", "john@example.com",
                "John", "Doe", LocalDate.of(1990, 1, 1), "+1234567890",
                "JDoe", true, true, true);

        when(backboneClient.checkEmail(anyString(), any(UUID.class))).thenReturn(ResponseEntity.ok().build());
        when(userCreateMapper.toBackbone(any(), any(), any(), anyString()))
                .thenThrow(new StandardException("Error", null));

        ResponseEntity<UserCreateResponse> result = service.create(request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
    }

    @Test
    @DisplayName("findUser - success with profile image")
    void findUser_success() {
        UUID userId = UUID.randomUUID();
        String token = "valid-token";
        BackboneUserGetResponse backboneUser = mock(BackboneUserGetResponse.class);
        BackboneProfileImageRefResponse profileImage = new BackboneProfileImageRefResponse("image-ref");
        GetUserResponse expected = mock(GetUserResponse.class);
        Set<UUID> businessIds = Set.of(UUID.randomUUID());

        when(backboneClient.findUserById(userId)).thenReturn(backboneUser);
        when(backboneClient.getProfileImageRef(anyString(), any(UUID.class)))
                .thenReturn(ResponseEntity.ok(profileImage));
        when(businessRepository.findIdCollectionByUserId(userId)).thenReturn(businessIds);
        when(getUserMapper.fromBackbone(backboneUser, "image-ref", businessIds)).thenReturn(expected);

        ResponseEntity<GetUserResponse> result = service.findUser(token, userId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(expected, result.getBody());
    }

    @Test
    @DisplayName("findUser - user not found returns NOT_FOUND")
    void findUser_notFound() {
        UUID userId = UUID.randomUUID();
        String token = "valid-token";

        when(backboneClient.findUserById(userId))
                .thenThrow(feignException(404, "Not Found"));

        ResponseEntity<GetUserResponse> result = service.findUser(token, userId);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertTrue(result.getHeaders().containsHeader("message-error"));
    }

    @Test
    @DisplayName("findUser - profile image is null uses empty string")
    void findUser_nullProfileImage() {
        UUID userId = UUID.randomUUID();
        String token = "valid-token";
        BackboneUserGetResponse backboneUser = mock(BackboneUserGetResponse.class);
        GetUserResponse expected = mock(GetUserResponse.class);

        when(backboneClient.findUserById(userId)).thenReturn(backboneUser);
        when(backboneClient.getProfileImageRef(anyString(), any(UUID.class)))
                .thenReturn(ResponseEntity.ok(null));
        when(businessRepository.findIdCollectionByUserId(userId)).thenReturn(Set.of());
        when(getUserMapper.fromBackbone(backboneUser, "", Set.of())).thenReturn(expected);

        ResponseEntity<GetUserResponse> result = service.findUser(token, userId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("update - success case")
    void update_success() {
        UUID userId = UUID.randomUUID();
        PutUserRequest request = new PutUserRequest("John", "Doe", "M", true, true,
                true, UUID.randomUUID(), "+1234567890", List.of(UUID.randomUUID()), true);
        BackboneUserGetResponse existingUser = mock(BackboneUserGetResponse.class);

        when(backboneClient.findUserById(userId)).thenReturn(existingUser);
        when(putUserMapper.toBackbone(any(UUID.class), any())).thenReturn(mock(BackboneUserUpdateRequest.class));
        when(backboneClient.userPartialUpdate(any(UUID.class), any())).thenReturn(ResponseEntity.ok().build());

        ResponseEntity<Void> result = service.update(userId, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("update - null request returns BAD_REQUEST")
    void update_nullRequest() {
        ResponseEntity<Void> result = service.update(UUID.randomUUID(), null);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("update - null userId returns BAD_REQUEST")
    void update_nullUserId() {
        PutUserRequest request = mock(PutUserRequest.class);
        ResponseEntity<Void> result = service.update(null, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("update - user not found returns NOT_FOUND")
    void update_userNotFound() {
        UUID userId = UUID.randomUUID();
        PutUserRequest request = new PutUserRequest(null, null, null, true, true, true,
                null, null, List.of(UUID.randomUUID()), true);

        when(backboneClient.findUserById(userId)).thenThrow(feignException(404, "Not Found"));

        ResponseEntity<Void> result = service.update(userId, request);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    @DisplayName("update - empty roleIds returns BAD_REQUEST")
    void update_emptyRoleIds() {
        UUID userId = UUID.randomUUID();
        PutUserRequest request = new PutUserRequest(null, null, null, true, true, true,
                null, null, List.of(), true);

        ResponseEntity<Void> result = service.update(userId, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("update - null role in roleIds returns BAD_REQUEST")
    void update_nullRoleId() {
        UUID userId = UUID.randomUUID();
        List<UUID> roleIds = new ArrayList<>();
        roleIds.add(UUID.randomUUID());
        roleIds.add(null);
        PutUserRequest request = new PutUserRequest(null, null, null, true, true, true,
                null, null, roleIds, true);

        ResponseEntity<Void> result = service.update(userId, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("update - duplicate roleIds returns BAD_REQUEST")
    void update_duplicateRoleIds() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        PutUserRequest request = new PutUserRequest(null, null, null, true, true, true,
                null, null, List.of(roleId, roleId), true);

        ResponseEntity<Void> result = service.update(userId, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("update - invalid UUID (all zeros) returns BAD_REQUEST")
    void update_invalidUUID() {
        UUID userId = UUID.randomUUID();
        UUID invalidUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        PutUserRequest request = new PutUserRequest(null, null, null, true, true, true,
                null, null, List.of(invalidUuid), true);

        ResponseEntity<Void> result = service.update(userId, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getHeaders().get("message"));
    }

    @Test
    @DisplayName("update - FeignException BAD_REQUEST propagates correctly")
    void update_feignBadRequest() {
        UUID userId = UUID.randomUUID();
        PutUserRequest request = new PutUserRequest(null, null, null, true, true, true,
                null, null, List.of(UUID.randomUUID()), true);
        BackboneUserGetResponse existingUser = mock(BackboneUserGetResponse.class);

        when(backboneClient.findUserById(userId)).thenReturn(existingUser);
        when(putUserMapper.toBackbone(any(UUID.class), any())).thenReturn(mock(BackboneUserUpdateRequest.class));
        when(backboneClient.userPartialUpdate(any(UUID.class), any()))
                .thenThrow(feignException(400, "Bad Request"));

        ResponseEntity<Void> result = service.update(userId, request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    @DisplayName("update - unexpected exception returns INTERNAL_SERVER_ERROR")
    void update_unexpectedException() {
        UUID userId = UUID.randomUUID();
        PutUserRequest request = new PutUserRequest(null, null, null, true, true, true,
                null, null, List.of(UUID.randomUUID()), true);

        when(backboneClient.findUserById(userId)).thenThrow(new RuntimeException("Unexpected"));

        ResponseEntity<Void> result = service.update(userId, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication - success returns NO_CONTENT")
    void delete_success() {
        UUID userId = UUID.randomUUID();

        when(backboneClient.deleteUserByUserIdAndApplicationId(any(UUID.class), eq(userId)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        ResponseEntity<Void> result = service.deleteUserByUserAndApplication(userId);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication - user not found returns NOT_FOUND")
    void delete_notFound() {
        UUID userId = UUID.randomUUID();

        when(backboneClient.deleteUserByUserIdAndApplicationId(any(UUID.class), eq(userId)))
                .thenThrow(feignException(404, "Not Found"));

        ResponseEntity<Void> result = service.deleteUserByUserAndApplication(userId);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication - forbidden returns FORBIDDEN")
    void delete_forbidden() {
        UUID userId = UUID.randomUUID();

        when(backboneClient.deleteUserByUserIdAndApplicationId(any(UUID.class), eq(userId)))
                .thenThrow(feignException(403, "Forbidden"));

        ResponseEntity<Void> result = service.deleteUserByUserAndApplication(userId);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication - bad request returns BAD_REQUEST")
    void delete_badRequest() {
        UUID userId = UUID.randomUUID();

        when(backboneClient.deleteUserByUserIdAndApplicationId(any(UUID.class), eq(userId)))
                .thenThrow(feignException(400, "Bad Request"));

        ResponseEntity<Void> result = service.deleteUserByUserAndApplication(userId);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication - forbidden returns FORBIDDEN (alternate)")
    void delete_forbidden_alternate() {
        UUID userId = UUID.randomUUID();

        when(backboneClient.deleteUserByUserIdAndApplicationId(any(UUID.class), eq(userId)))
                .thenThrow(feignException(403, "Forbidden"));

        ResponseEntity<Void> result = service.deleteUserByUserAndApplication(userId);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    @Test
    @DisplayName("generateFourDigitNumber - generates valid 4-digit number")
    void generateFourDigitNumber_valid() {
        int result = service.generateFourDigitNumber();

        assertTrue(result >= 1000 && result <= 9999);
    }

    @Test
    @DisplayName("generateFourDigitNumber - generates different numbers")
    void generateFourDigitNumber_random() {
        Set<Integer> generated = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            generated.add(service.generateFourDigitNumber());
        }
        // With 100 iterations, we should get multiple different numbers
        assertTrue(generated.size() > 1);
    }

    // Helper to build a minimal feign Request used by Feign Response
    private static Request mockFeignRequest() {
        return Request.create(Request.HttpMethod.GET, "http://unit.test", Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
    }

    private static FeignException feignException(int status, String reason) {
        Response resp = Response.builder()
                .status(status)
                .reason(reason)
                .request(mockFeignRequest())
                .headers(Map.of("message", List.of(reason)))
                .body(new byte[0])
                .build();
        return FeignException.errorStatus("unit", resp);
    }
}
