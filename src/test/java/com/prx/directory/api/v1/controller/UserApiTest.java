package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.UserService;
import com.prx.directory.api.v1.to.GetUserResponse;
import com.prx.directory.api.v1.to.UserCreateRequest;
import com.prx.directory.api.v1.to.UserCreateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(value = {SpringExtension.class})
class UserApiTest {

    private final UserApi userApi = new UserApi() {
        @Override
        public UserService getService() {
            return userService;
        }
    };

    @Mock
    private UserService userService;

    @Test
    @DisplayName("createUser should return OK status with valid request")
    void createUserShouldReturnOkStatusWithValidRequest() {
        String password = "abc123";
        String displayName = "Pepeto";
        String email = "user@domain.ext";
        String firstName = "John";
        String lastName = "Connor";
        LocalDate birthDate = LocalDate.now();
        String phone = "5869995852";
        UserCreateRequest request = new UserCreateRequest(
                password,
                email,
                firstName,
                lastName,
                birthDate,
                phone,
                displayName,
                true,
                true,
                true
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
                true,
                true,
                true
                );
        when(userService.create(request)).thenReturn(ResponseEntity.ok(response));

        ResponseEntity<UserCreateResponse> result = userApi.createUserPost(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("createUser should return BAD_REQUEST status with null request")
    void createUserShouldReturnBadRequestStatusWithNullRequest() {
        when(userService.create(Mockito.any())).thenReturn(ResponseEntity.badRequest().build());
        ResponseEntity<UserCreateResponse> result = userApi.createUserPost(null);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    @DisplayName("createUser should return BAD_REQUEST status with invalid email")
    void createUserShouldReturnBadRequestStatusWithInvalidEmail() {
        String displayName = "Pepeto";
        String email = "user@domain.ext";
        String firstName = "John";
        String lastName = "Connor";
        LocalDate birthDate = LocalDate.now();
        String phone = "5869995852";
        UserCreateRequest request = new UserCreateRequest(
                null,
                email,
                firstName,
                lastName,
                birthDate,
                phone,
                displayName,
                true,
                true,
                true
        );

        when(userService.create(request)).thenReturn(ResponseEntity.badRequest().build());

        ResponseEntity<UserCreateResponse> result = userApi.createUserPost(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    @DisplayName("createUser should return CONFLICT status with existing username")
    void createUserShouldReturnConflictStatusWithExistingUsername() {
        String password = "abc123";
        String displayName = "Pepeto";
        String email = "user@domain.ext";
        String firstName = "John";
        String lastName = "Connor";
        LocalDate birthDate = LocalDate.now();
        String phone = "5869995852";
        UserCreateRequest request = new UserCreateRequest(
                password,
                email,
                firstName,
                lastName,
                birthDate,
                phone,
                displayName,
                true,
                true,
                true
        );

        when(userService.create(request)).thenReturn(ResponseEntity.status(HttpStatus.CONFLICT).build());

        ResponseEntity<UserCreateResponse> result = userApi.createUserPost(request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
    }

    @Test
    @DisplayName("findUser should return OK status with valid ID")
    void findUserShouldReturnOkStatusWithValidID() {
        UUID userId = UUID.randomUUID();
        GetUserResponse response = new GetUserResponse(
                userId,
                "John",
                "john.connor@example.com",
                "John",
                "Marcus",
                "Connor",
                "John M",
                UUID.randomUUID(),
                "(+1) 4167487564",
                LocalDate.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                true,
                true,
                true,
                true,
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        when(userService.findUser(userId)).thenReturn(ResponseEntity.ok(response));

        ResponseEntity<GetUserResponse> result = userApi.userGet(userId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("findUser should return NOT_FOUND status with non-existent ID")
    void findUserShouldReturnNotFoundStatusWithNonExistentID() {
        UUID userId = UUID.randomUUID();
        when(userService.findUser(userId)).thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

        ResponseEntity<GetUserResponse> result = userApi.userGet(userId);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }


    @Test
    @DisplayName("findUser should return INTERNAL_SERVER_ERROR status with server error")
    void findUserShouldReturnInternalServerErrorStatusWithServerError() {
        UUID userId = UUID.randomUUID();
        when(userService.findUser(userId)).thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        ResponseEntity<GetUserResponse> result = userApi.userGet(userId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }


}
