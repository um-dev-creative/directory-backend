package com.prx.directory.api.v1.service;

import com.prx.commons.general.pojo.Application;
import com.prx.commons.general.pojo.Person;
import com.prx.commons.general.pojo.Role;
import com.prx.directory.api.v1.to.UseGetResponse;
import com.prx.directory.api.v1.to.UserCreateRequest;
import com.prx.directory.api.v1.to.UserCreateResponse;
import com.prx.directory.client.backbone.BackboneClient;
import com.prx.directory.client.backbone.to.BackboneUserCreateRequest;
import com.prx.directory.client.backbone.to.BackboneUserCreateResponse;
import com.prx.directory.client.backbone.to.BackboneUserGetResponse;
import com.prx.directory.kafka.producer.EmailMessageProducerService;
import com.prx.directory.mapper.UserCreateMapper;
import com.prx.directory.mapper.UserGetMapper;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.prx.directory.constant.DirectoryAppConstants.MESSAGE_ERROR_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(value = {SpringExtension.class})
class UserServiceImplTest {

    @Mock
    BackboneClient backboneClient;
    @Mock
    UserCreateMapper userCreateMapper;
    @Mock
    UserGetMapper userGetMapper;
    @Mock
    EmailMessageProducerService emailMessageProducerService;
    @InjectMocks
    UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "applicationId", "123e4567-e89b-12d3-a456-426614174000");
        ReflectionTestUtils.setField(userService, "initialRoleId", "123e4567-e89b-12d3-a456-426614174001");
    }

    @Test
    @DisplayName("Create User")
    void createUser() {
        UserCreateRequest request = new UserCreateRequest(
                "abc123",
                "user@domain.ext",
                "John",
                "Connor",
                LocalDate.parse("1984-05-12"),
                "547424"
        );
        Person person = new Person();
        person.setGender("M");
        person.setFirstName("John");
        person.setLastName("Connor");
        person.setBirthdate(LocalDate.parse("1984-05-12"));
        person.setMiddleName("A");

        BackboneUserCreateRequest backboneRequest = new BackboneUserCreateRequest(
                UUID.randomUUID(),
                "alias",
                "password",
                "user@domain.ext",
                true,
                person,
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        BackboneUserCreateResponse backboneResponse = new BackboneUserCreateResponse(
                UUID.randomUUID(),
                "alias",
                "user@domain.ext",
                LocalDateTime.now(),
                LocalDateTime.now(),
                true,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        UserCreateResponse response = new UserCreateResponse(UUID.randomUUID(),
                "john1",
                "user@domain.ext",
                LocalDateTime.now(),
                LocalDateTime.now(),
                true,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
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
        UserCreateRequest request = new UserCreateRequest(
                "abc123",
                "invalid-email",
                "John",
                "Connor",
                LocalDate.parse("1984-05-12"),
                "547424"
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
        UserCreateRequest request = new UserCreateRequest(
                "abc123",
                "user@domain.ext",
                "John",
                "Connor",
                LocalDate.parse("1984-05-12"),
                "547424"
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
        UserCreateRequest request = new UserCreateRequest(
                "abc123",
                null,
                "John",
                "Connor",
                LocalDate.parse("1984-05-12"),
                "547424"
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
        UserCreateRequest request = new UserCreateRequest(
                null,
                "user@domain.ext",
                "John",
                "Connor",
                LocalDate.parse("1984-05-12"),
                "547424"
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
        UserCreateRequest request = new UserCreateRequest(
                "abc123",
                "user@domain.ext",
                "John",
                "Connor",
                LocalDate.parse("1984-05-12"),
                "547424"
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
        UUID userId = UUID.randomUUID();
        Person person = new Person();
        person.setGender("M");
        person.setFirstName("John");
        person.setLastName("Connor");
        person.setBirthdate(LocalDate.parse("1984-05-12"));
        person.setMiddleName("Marcus");
        Application application = new Application();
        application.setId(UUID.randomUUID());
        Role role = new Role();
        role.setId(UUID.randomUUID());

        UseGetResponse expectedResponse = new UseGetResponse(
                userId,
                "jconnor",
                "john.connor@example.com",
                "(+1) 4167389402",
                "John",
                "Marcus",
                "Connor",
                "Marcus",
                "M",
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


        when(backboneClient.find(userId)).thenReturn(backboneResponse);
        when(userGetMapper.fromBackbone(backboneResponse)).thenReturn(expectedResponse);

        ResponseEntity<UseGetResponse> response = userService.findUser(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
    }

    @Test
    @DisplayName("Find User with Nonexistent ID")
    void findUserWithNonexistentID() {
        UUID userId = UUID.randomUUID();

        Request requestFeign = Request.create(Request.HttpMethod.GET, "url", Map.of(), null, null, null);
        when(backboneClient.find(any())).thenThrow(new FeignException.NotFound("User not found", requestFeign, null, null));

        ResponseEntity<UseGetResponse> response = userService.findUser(userId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found.", response.getHeaders().getFirst("message-error"));
    }

    @Test
    @DisplayName("Find User with FeignException")
    void findUserWithFeignException() {
        UUID userId = UUID.randomUUID();

        Request requestFeign = Request.create(Request.HttpMethod.GET, "url", Map.of(), null, null, null);
        when(backboneClient.find(any())).thenThrow(new FeignException
                .InternalServerError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), requestFeign, null, null));

        ResponseEntity<UseGetResponse> response = userService.findUser(userId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
