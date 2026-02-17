package com.prx.directory.api.v1.service;

import com.prx.commons.general.pojo.Application;
import com.prx.commons.general.pojo.Person;
import com.prx.commons.general.pojo.Role;
import com.prx.directory.api.v1.to.GetUserResponse;
import com.prx.directory.api.v1.to.UserCreateRequest;
import com.prx.directory.api.v1.to.UserCreateResponse;
import com.prx.directory.api.v1.to.PutUserRequest;
import com.prx.directory.client.backbone.BackboneClient;
import com.prx.directory.client.backbone.to.BackboneProfileImageRefResponse;
import com.prx.directory.client.backbone.to.BackboneUserCreateRequest;
import com.prx.directory.client.backbone.to.BackboneUserCreateResponse;
import com.prx.directory.client.backbone.to.BackboneUserGetResponse;
import com.prx.directory.client.backbone.to.BackboneUserUpdateRequest;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.kafka.producer.EmailMessageProducerService;
import com.prx.directory.mapper.GetUserMapper;
import com.prx.directory.mapper.UserCreateMapper;
import com.prx.directory.mapper.PutUserMapper;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.prx.directory.constant.DirectoryAppConstants.MESSAGE_ERROR_HEADER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(value = {SpringExtension.class})
class UserServiceImplTest {

    @Mock
    BackboneClient backboneClient;
    @Mock
    UserCreateMapper userCreateMapper;
    @Mock
    GetUserMapper getUserMapper;
    @Mock
    BusinessRepository businessRepository;
    @Mock
    EmailMessageProducerService emailMessageProducerService;
    @Mock
    PutUserMapper putUserMapper;
    @InjectMocks
    UserServiceImpl userService;

    private UUID userId;
    private UUID applicationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        String applicationIdString = applicationId.toString();
        ReflectionTestUtils.setField(userService, "applicationIdString", applicationIdString);
        ReflectionTestUtils.setField(userService, "initialRoleId", "123e4567-e89b-12d3-a456-426614174001");
    }

    @Test
    @DisplayName("Create User")
    void createUser() {
        String password = "abc123";
        String email = "user@domain.ext";
        String firstname = "John";
        String lastname = "Connor";
        LocalDate dateOfBirth = LocalDate.parse("1984-05-12");
        String phoneNumber = "547424";
        String displayName = "John Connor";
        Boolean notificationSms = true;
        Boolean notificationEmail = true;
        Boolean privacyDataOutActive = false;
        UserCreateRequest request = new UserCreateRequest(
                password,
                email,
                firstname,
                lastname,
                dateOfBirth,
                phoneNumber,
                displayName,
                notificationSms,
                notificationEmail,
                privacyDataOutActive
        );
        Person person = new Person();
        person.setGender("M");
        person.setFirstName(firstname);
        person.setLastName(lastname);
        person.setBirthdate(dateOfBirth);
        person.setMiddleName("A");
        BackboneUserCreateRequest backboneRequest = new BackboneUserCreateRequest(
                UUID.randomUUID(),
                "alias",
                password,
                email,
                true,
                person,
                UUID.randomUUID(),
                UUID.randomUUID(),
                displayName,
                notificationSms,
                notificationEmail,
                privacyDataOutActive
        );
        BackboneUserCreateResponse backboneResponse = new BackboneUserCreateResponse(
                UUID.randomUUID(),
                "alias",
                email,
                LocalDateTime.now(),
                LocalDateTime.now(),
                true,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                displayName,
                notificationSms,
                notificationEmail,
                privacyDataOutActive
        );
        UserCreateResponse response = new UserCreateResponse(UUID.randomUUID(),
                "john1",
                email,
                LocalDateTime.now(),
                LocalDateTime.now(),
                true,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                displayName,
                notificationSms,
                notificationEmail,
                privacyDataOutActive
        );
        when(backboneClient.checkEmail(anyString(), any())).thenReturn(ResponseEntity.ok().build());
        when(backboneClient.checkAlias(anyString(), any())).thenReturn(ResponseEntity.ok().build());
        when(userCreateMapper.toBackbone(any(), any(), any(), any())).thenReturn(backboneRequest);
        when(backboneClient.post(any())).thenReturn(backboneResponse);
        when(userCreateMapper.fromBackbone(any())).thenReturn(response);
        ResponseEntity<UserCreateResponse> result = userService.create(request);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }


    @Test
    @DisplayName("Create User with Invalid Email")
    void createUserWithInvalidEmail() {
        String password = "abc123";
        String email = "gshdj@gstn.com";
        String firstname = "John";
        String lastname = "Connor";
        LocalDate dateOfBirth = LocalDate.parse("1984-05-12");
        String phoneNumber = "547424";
        String displayName = "John Connor";
        Boolean notificationSms = true;
        Boolean notificationEmail = true;
        Boolean privacyDataOutActive = false;
        UserCreateRequest request = new UserCreateRequest(
                password,
                email,
                firstname,
                lastname,
                dateOfBirth,
                phoneNumber,
                displayName,
                notificationSms,
                notificationEmail,
                privacyDataOutActive
        );

        when(backboneClient.checkEmail(anyString(), any()))
                .thenThrow(new FeignException.Conflict("Invalid email",
                        Request.create(Request.HttpMethod.POST, "url", Map.of(),
                                null, null, null), null, null));
        ResponseEntity<UserCreateResponse> result = userService.create(request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        assertEquals("Email already exists", result.getHeaders().getFirst(MESSAGE_ERROR_HEADER));
    }

    @Test
    @DisplayName("Create User with Existing Alias")
    void createUserWithExistingAlias() {
        String password = "abc123";
        String email = "gshdj@gstn.com";
        String firstname = "John";
        String lastname = "Connor";
        LocalDate dateOfBirth = LocalDate.parse("1984-05-12");
        String phoneNumber = "547424";
        String displayName = "John Connor";
        Boolean notificationSms = true;
        Boolean notificationEmail = true;
        Boolean privacyDataOutActive = false;
        UserCreateRequest request = new UserCreateRequest(
                password,
                email,
                firstname,
                lastname,
                dateOfBirth,
                phoneNumber,
                displayName,
                notificationSms,
                notificationEmail,
                privacyDataOutActive
        );

        when(backboneClient.checkEmail(anyString(), any())).thenReturn(ResponseEntity.ok().build());
        Request requestFeign = Request.create(Request.HttpMethod.POST, "url", Map.of(), null, null, null);
        when(backboneClient.checkAlias(anyString(), any())).thenThrow(new FeignException.Conflict("Alias already exists", requestFeign, null, null));

        ResponseEntity<UserCreateResponse> result = userService.create(request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        assertEquals("Error creating user", result.getHeaders().getFirst(MESSAGE_ERROR_HEADER));
    }

    @Test
    @DisplayName("Create User with Null Email")
    void createUserWithNullEmail() {
        String password = "abc123";
        String email = "gshdj@gstn.com";
        String firstname = "John";
        String lastname = "Connor";
        LocalDate dateOfBirth = LocalDate.parse("1984-05-12");
        String phoneNumber = "547424";
        String displayName = "John Connor";
        Boolean notificationSms = true;
        Boolean notificationEmail = true;
        Boolean privacyDataOutActive = false;
        UserCreateRequest request = new UserCreateRequest(
                password,
                email,
                firstname,
                lastname,
                dateOfBirth,
                phoneNumber,
                displayName,
                notificationSms,
                notificationEmail,
                privacyDataOutActive
        );

        when(backboneClient.checkEmail(anyString(), any())).thenReturn(ResponseEntity.status(HttpStatus.CONFLICT).build());
        Request requestFeign = Request.create(Request.HttpMethod.POST, "url", Map.of(), null, null, null);
        when(backboneClient.checkAlias(anyString(), any())).thenThrow(new FeignException.BadRequest("Error creating user", requestFeign, null, null));
        ResponseEntity<UserCreateResponse> result = userService.create(request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        assertEquals("Error creating user", result.getHeaders().getFirst(MESSAGE_ERROR_HEADER));
    }

    @Test
    @DisplayName("Create User with Null Alias")
    void createUserWithNullAlias() {
        String password = "abc123";
        String email = "gshdj@gstn.com";
        String firstname = "John";
        String lastname = "Connor";
        LocalDate dateOfBirth = LocalDate.parse("1984-05-12");
        String phoneNumber = "547424";
        String displayName = "John Connor";
        Boolean notificationSms = true;
        Boolean notificationEmail = true;
        Boolean privacyDataOutActive = false;
        UserCreateRequest request = new UserCreateRequest(
                password,
                email,
                firstname,
                lastname,
                dateOfBirth,
                phoneNumber,
                displayName,
                notificationSms,
                notificationEmail,
                privacyDataOutActive
        );
        when(backboneClient.checkEmail(anyString(), any())).thenReturn(ResponseEntity.ok().build());
        Request requestFeign = Request.create(Request.HttpMethod.POST, "url", Map.of(), null, null, null);
        when(backboneClient.checkAlias(anyString(), any())).thenThrow(new FeignException.BadRequest("Error creating user", requestFeign, null, null));

        ResponseEntity<UserCreateResponse> result = userService.create(request);
        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        assertEquals("Error creating user", result.getHeaders().getFirst(MESSAGE_ERROR_HEADER));
    }

    @Test
    @DisplayName("Create User with Existing Email")
    void createUserWithExistingEmail() {
        String password = "abc123";
        String email = "gshdj@gstn.com";
        String firstname = "John";
        String lastname = "Connor";
        LocalDate dateOfBirth = LocalDate.parse("1984-05-12");
        String phoneNumber = "547424";
        String displayName = "John Connor";
        Boolean notificationSms = true;
        Boolean notificationEmail = true;
        Boolean privacyDataOutActive = false;
        UserCreateRequest request = new UserCreateRequest(
                password,
                email,
                firstname,
                lastname,
                dateOfBirth,
                phoneNumber,
                displayName,
                notificationSms,
                notificationEmail,
                privacyDataOutActive
        );

        Request requestFeign = Request.create(Request.HttpMethod.POST, "url", Map.of(), null, null, null);

        when(backboneClient.checkEmail(anyString(), any())).thenThrow(new FeignException.Conflict("Email already exists", requestFeign, null, null));

        ResponseEntity<UserCreateResponse> result = userService.create(request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        assertEquals("Email already exists", result.getHeaders().getFirst(MESSAGE_ERROR_HEADER));
    }

    @Test
    @DisplayName("Find User Successfully")
    void findUserSuccessfully() {
        String profileImageRef = "LTHB/3171803c-bb08-4eb0-8821-b6ebe5948f48-250723104035.jpg";
        Person person = new Person();
        person.setGender("M");
        person.setFirstName("John");
        person.setLastName("Connor");
        person.setBirthdate(LocalDate.parse("1984-05-12"));
        person.setMiddleName("Marcus");
        Application application = new Application();
        application.setId(UUID.randomUUID());
        Role role = new Role();
        role.setName("Role Name");
        role.setDescription("Role Description");
        role.setActive(true);
        role.setId(UUID.randomUUID());

        GetUserResponse expectedResponse = new GetUserResponse(
                userId,
                "jconnor",
                "john.connor@example.com",
                "John",
                "Marcus",
                "Connor",
                "M",
                profileImageRef,
                UUID.randomUUID(),
                "(+1) 4167389402",
                Collections.emptySet(),
                LocalDate.parse("1984-05-12"),
                LocalDateTime.now(),
                LocalDateTime.now(),
                true,
                true,
                true,
                true,
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        BackboneUserGetResponse backboneResponse = new BackboneUserGetResponse(
                userId,
                "jconnor",
                "abcsasa",
                "jconnor@mail.com",
                "John",
                LocalDateTime.now(),
                LocalDateTime.now(),
                true,
                true,
                true,
                true,
                person,
                List.of(role),
                List.of(application)
        );
        ResponseEntity<BackboneProfileImageRefResponse> expectedReference = ResponseEntity.ok(new BackboneProfileImageRefResponse("imageRef123"));

        when(backboneClient.findUserById(userId)).thenReturn(backboneResponse);
        when(businessRepository.findIdCollectionByUserId(any(UUID.class))).thenReturn(Collections.emptySet());
        when(backboneClient.getProfileImageRef(anyString(), any(UUID.class))).thenReturn(expectedReference);
        when(getUserMapper.fromBackbone(backboneResponse, profileImageRef, Collections.emptySet())).thenReturn(expectedResponse);

        ResponseEntity<GetUserResponse> response = userService.findUser("token", userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Find User with Nonexistent ID")
    void findUserWithNonexistentID() {

        Request requestFeign = Request.create(Request.HttpMethod.GET, "url", Map.of(), null, null, null);
        when(backboneClient.findUserById(any())).thenThrow(new FeignException.NotFound("User not found", requestFeign, null, null));

        ResponseEntity<GetUserResponse> response = userService.findUser("token", userId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found.", response.getHeaders().getFirst("message-error"));
    }

    @Test
    @DisplayName("Find User with FeignException")
    void findUserWithFeignException() {

        Request requestFeign = Request.create(Request.HttpMethod.GET, "url", Map.of(), null, null, null);
        when(backboneClient.findUserById(any())).thenThrow(new FeignException
                .InternalServerError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), requestFeign, null, null));

        ResponseEntity<GetUserResponse> response = userService.findUser("token", userId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication should return NO_CONTENT when user is deleted successfully")
    void deleteUserByUserAndApplicationShouldReturnNoContentWhenUserDeletedSuccessfully() {
        // Arrange
        ResponseEntity<Void> backboneResponse = new ResponseEntity<>(HttpStatus.NO_CONTENT);
        when(backboneClient.deleteUserByUserIdAndApplicationId(any(UUID.class), any(UUID.class)))
                .thenReturn(backboneResponse);

        // Act
        ResponseEntity<Void> response = userService.deleteUserByUserAndApplication(userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(backboneClient).deleteUserByUserIdAndApplicationId(applicationId, userId);
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication should return NOT_FOUND when user does not exist")
    void deleteUserByUserAndApplicationShouldReturnNotFoundWhenUserDoesNotExist() {
        // Arrange
        Request requestFeign = Request.create(Request.HttpMethod.DELETE, "url", Map.of(), null, null, null);
        when(backboneClient.deleteUserByUserIdAndApplicationId(any(UUID.class), any(UUID.class))).thenThrow(new FeignException
                .NotFound(HttpStatus.NOT_FOUND.getReasonPhrase(), requestFeign, null, null));

        // Act
        ResponseEntity<Void> response = userService.deleteUserByUserAndApplication(userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(backboneClient).deleteUserByUserIdAndApplicationId(applicationId, userId);
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication should return FORBIDDEN when access is denied")
    void deleteUserByUserAndApplicationShouldReturnForbiddenWhenAccessDenied() {
        // Arrange

        Request requestFeign = Request.create(Request.HttpMethod.DELETE, "url", Map.of(), null, null, null);
        when(backboneClient.deleteUserByUserIdAndApplicationId(any(UUID.class), any(UUID.class))).thenThrow(new FeignException
                .Forbidden(HttpStatus.FORBIDDEN.getReasonPhrase(), requestFeign, null, null));

        // Act
        ResponseEntity<Void> response = userService.deleteUserByUserAndApplication(userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(backboneClient).deleteUserByUserIdAndApplicationId(applicationId, userId);
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication should return INTERNAL_SERVER_ERROR for unexpected status codes")
    void deleteUserByUserAndApplicationShouldReturnInternalServerErrorForUnexpectedStatusCodes() {
        // Arrange
        Request requestFeign = Request.create(Request.HttpMethod.DELETE, "url", Map.of(), null, null, null);
        when(backboneClient.deleteUserByUserIdAndApplicationId(any(UUID.class), any(UUID.class))).thenThrow(new FeignException
                .BadRequest(HttpStatus.BAD_REQUEST.getReasonPhrase(), requestFeign, null, null));

        // Act
        ResponseEntity<Void> response = userService.deleteUserByUserAndApplication(userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(backboneClient).deleteUserByUserIdAndApplicationId(applicationId, userId);
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication should return INTERNAL_SERVER_ERROR when BackboneClient throws RuntimeException")
    void deleteUserByUserAndApplicationShouldReturnInternalServerErrorWhenBackboneClientThrowsRuntimeException() {
        // Arrange
        when(backboneClient.deleteUserByUserIdAndApplicationId(any(UUID.class), any(UUID.class)))
                .thenThrow(new RuntimeException("External service error"));

        // Act
        ResponseEntity<Void> response = userService.deleteUserByUserAndApplication(userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(backboneClient).deleteUserByUserIdAndApplicationId(applicationId, userId);
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication should return INTERNAL_SERVER_ERROR when BackboneClient throws FeignException")
    void deleteUserByUserAndApplicationShouldReturnInternalServerErrorWhenBackboneClientThrowsFeignException() {
        // Arrange
        FeignException feignException = mock(FeignException.class);
        when(backboneClient.deleteUserByUserIdAndApplicationId(any(UUID.class), any(UUID.class)))
                .thenThrow(feignException);

        // Act
        ResponseEntity<Void> response = userService.deleteUserByUserAndApplication(userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(backboneClient).deleteUserByUserIdAndApplicationId(applicationId, userId);
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication should handle null response from BackboneClient")
    void deleteUserByUserAndApplicationShouldHandleNullResponseFromBackboneClient() {
        // Arrange
        when(backboneClient.deleteUserByUserIdAndApplicationId(any(UUID.class), any(UUID.class)))
                .thenReturn(null);

        // Act
        ResponseEntity<Void> response = userService.deleteUserByUserAndApplication(userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(backboneClient).deleteUserByUserIdAndApplicationId(applicationId, userId);
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication should handle UNAUTHORIZED status from BackboneClient")
    void deleteUserByUserAndApplicationShouldHandleUnauthorizedStatusFromBackboneClient() {
        // Arrange
        ResponseEntity<Void> backboneResponse = new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        when(backboneClient.deleteUserByUserIdAndApplicationId(any(UUID.class), any(UUID.class)))
                .thenReturn(backboneResponse);

        // Act
        ResponseEntity<Void> response = userService.deleteUserByUserAndApplication(userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
        verify(backboneClient).deleteUserByUserIdAndApplicationId(applicationId, userId);
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication should handle SERVICE_UNAVAILABLE status from BackboneClient")
    void deleteUserByUserAndApplicationShouldHandleServiceUnavailableStatusFromBackboneClient() {
        // Arrange
        ResponseEntity<Void> backboneResponse = new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        when(backboneClient.deleteUserByUserIdAndApplicationId(any(UUID.class), any(UUID.class)))
                .thenReturn(backboneResponse);

        // Act
        ResponseEntity<Void> response = userService.deleteUserByUserAndApplication(userId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
        verify(backboneClient).deleteUserByUserIdAndApplicationId(applicationId, userId);
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication should handle null userId gracefully")
    void deleteUserByUserAndApplicationShouldHandleNullUserIdGracefully() {
        // Arrange
        when(backboneClient.deleteUserByUserIdAndApplicationId(any(UUID.class), any(UUID.class)))
                .thenThrow(new IllegalArgumentException("User ID cannot be null"));

        // Act
        ResponseEntity<Void> response = userService.deleteUserByUserAndApplication(null);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(backboneClient).deleteUserByUserIdAndApplicationId(applicationId, null);
    }

    @Test
    @DisplayName("deleteUserByUserAndApplication should handle malformed applicationId configuration")
    void deleteUserByUserAndApplicationShouldHandleMalformedApplicationIdConfiguration() {
        // Arrange
        ReflectionTestUtils.setField(userService, "applicationIdString", "invalid-uuid");

        // Act & Assert
        try {
            userService.deleteUserByUserAndApplication(userId);
        } catch (IllegalArgumentException e) {
            // Expected behavior when UUID.fromString fails
            assertEquals("Invalid UUID string: invalid-uuid", e.getMessage());
        }
    }

    @Test
    @DisplayName("update should return BAD_REQUEST when request is null")
    void update_nullRequest_returnsBadRequest() {
        ResponseEntity<Void> resp = userService.update(userId, null);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Request body is missing.", resp.getHeaders().getFirst("message"));
    }

    @Test
    @DisplayName("update should return BAD_REQUEST when userId is null")
    void update_nullUserId_returnsBadRequest() {
        PutUserRequest req = new PutUserRequest("A","B","D", true, true, false, null, "123", List.of(UUID.randomUUID()), true);
        ResponseEntity<Void> resp = userService.update(null, req);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("User ID is required.", resp.getHeaders().getFirst("message"));
    }

    @Test
    @DisplayName("update should validate empty roleIds and return BAD_REQUEST")
    void update_emptyRoleIds_returnsBadRequest() {
        PutUserRequest req = new PutUserRequest("A","B","D", true, true, false, null, "123", Collections.emptyList(), true);
        ResponseEntity<Void> resp = userService.update(userId, req);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Cannot remove all roles from user. At least one role is required.", resp.getHeaders().getFirst("message"));
    }

    @Test
    @DisplayName("update should detect duplicate roleIds and return BAD_REQUEST")
    void update_duplicateRoleIds_returnsBadRequest() {
        UUID r = UUID.randomUUID();
        List<UUID> roleIds = List.of(r, r);
        PutUserRequest req = new PutUserRequest("A","B","D", true, true, false, null, "123", roleIds, true);
        ResponseEntity<Void> resp = userService.update(userId, req);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Role collection contains duplicate values.", resp.getHeaders().getFirst("message"));
    }

    @Test
    @DisplayName("update should detect invalid zero UUID and return BAD_REQUEST")
    void update_invalidZeroRoleId_returnsBadRequest() {
        UUID zero = UUID.fromString("00000000-0000-0000-0000-000000000000");
        List<UUID> roleIds = List.of(zero);
        PutUserRequest req = new PutUserRequest("A","B","D", true, true, false, null, "123", roleIds, true);
        ResponseEntity<Void> resp = userService.update(userId, req);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Role collection contains invalid role ID.", resp.getHeaders().getFirst("message"));
    }

    @Test
    @DisplayName("update should return NOT_FOUND when backbone findUserById returns null")
    void update_userNotFound_whenFindReturnsNull() {
        PutUserRequest req = new PutUserRequest("A","B","D", true, true, false, null, "123", List.of(UUID.randomUUID()), true);
        when(backboneClient.findUserById(any())).thenReturn(null);

        ResponseEntity<Void> resp = userService.update(userId, req);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("User not found.", resp.getHeaders().getFirst("message"));
    }

    @Test
    @DisplayName("update should call putUserMapper and backbone userPartialUpdate on success")
    void update_success_callsUserPartialUpdate() {
        PutUserRequest req = new PutUserRequest("A","B","D", true, true, false, null, "123", List.of(UUID.randomUUID()), true);
        // mock existing user returned
        BackboneUserGetResponse existing = mock(BackboneUserGetResponse.class);
        when(backboneClient.findUserById(any())).thenReturn(existing);
        // prepare backbone update request
        BackboneUserUpdateRequest bReq = new BackboneUserUpdateRequest(UUID.randomUUID(), true, true, true, List.of(UUID.randomUUID()));
        when(putUserMapper.toBackbone(any(), any())).thenReturn(bReq);
        ResponseEntity<Void> bResp = ResponseEntity.noContent().build();
        when(backboneClient.userPartialUpdate(any(), any())).thenReturn(bResp);

        ResponseEntity<Void> resp = userService.update(userId, req);
        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(putUserMapper).toBackbone(any(), eq(req));
        verify(backboneClient).userPartialUpdate(eq(userId), eq(bReq));
    }

    @Test
    @DisplayName("update should return BAD_REQUEST when userPartialUpdate throws FeignException.BadRequest")
    void update_userPartialUpdate_badRequest() {
        PutUserRequest req = new PutUserRequest("A","B","D", true, true, false, null, "123", List.of(UUID.randomUUID()), true);
        BackboneUserGetResponse existing = mock(BackboneUserGetResponse.class);
        when(backboneClient.findUserById(any())).thenReturn(existing);
        BackboneUserUpdateRequest bReq = new BackboneUserUpdateRequest(UUID.randomUUID(), true, true, true, List.of(UUID.randomUUID()));
        when(putUserMapper.toBackbone(any(), any())).thenReturn(bReq);
        Request r = Request.create(Request.HttpMethod.PUT, "url", Map.of(), null, null, null);
        when(backboneClient.userPartialUpdate(any(), any())).thenThrow(new FeignException.BadRequest("bad", r, null, null));

        ResponseEntity<Void> resp = userService.update(userId, req);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Invalid request data.", resp.getHeaders().getFirst("message"));
    }

    @Test
    @DisplayName("generateVerificationCode returns pattern ####-####")
    void generateVerificationCode_pattern() throws Exception {
        // invoke private method via reflection
        String code = (String) org.springframework.test.util.ReflectionTestUtils.invokeMethod(userService, "generateVerificationCode");
        assertNotNull(code);
        assertTrue(code.matches("\\d{4}-\\d{4}"));
    }
}
