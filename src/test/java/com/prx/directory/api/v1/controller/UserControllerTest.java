package com.prx.directory.api.v1.controller;

import com.prx.directory.api.v1.service.UserService;
import com.prx.directory.api.v1.to.UserCreateRequest;
import com.prx.directory.api.v1.to.UserCreateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(value = {SpringExtension.class})
class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    @Test
    @DisplayName("post should return OK status with valid request")
    void createUserPostShouldReturnOkStatusWithValidRequest() {
        UserCreateRequest request = new UserCreateRequest(
                "abc123",
                "user@domain.ext",
                "John",
                "Connor",
                LocalDate.now(),
                "5869995852"
        );
        UserCreateResponse response = new UserCreateResponse(
                UUID.randomUUID(),
                "abc123",
                "user@domain.ext",
                LocalDateTime.now(),
                LocalDateTime.now(),
                true,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        when(userService.create(request)).thenReturn(ResponseEntity.ok(response));

        ResponseEntity<UserCreateResponse> result = userController.createUserPost(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("post should return BAD_REQUEST status with null request")
    void createUserPostShouldReturnBadRequestStatusWithNullRequest() {
        when(userService.create(null)).thenReturn(ResponseEntity.badRequest().build());

        ResponseEntity<UserCreateResponse> result = userController.createUserPost(null);
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    @DisplayName("post should return BAD_REQUEST status with invalid email")
    void createUserPostShouldReturnBadRequestStatusWithInvalidEmail() {
        UserCreateRequest request = new UserCreateRequest(
                "abc123",
                "userdomain.ext",
                "John",
                "Connor",
                LocalDate.now(),
                "5869995852"
        );
        when(userService.create(request)).thenReturn(ResponseEntity.badRequest().build());

        ResponseEntity<UserCreateResponse> result = userController.createUserPost(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    @DisplayName("post should return CONFLICT status with existing username")
    void createUserPostShouldReturnConflictStatusWithExistingUsername() {
        UserCreateRequest request = new UserCreateRequest(
                "abc123",
                "user@domain.ext",
                "John",
                "Connor",
                LocalDate.now(),
                "5869995852"
        );
        when(userService.create(request)).thenReturn(ResponseEntity.status(HttpStatus.CONFLICT).build());

        ResponseEntity<UserCreateResponse> result = userController.createUserPost(request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
    }

    @Test
    @DisplayName("post should return BAD_REQUEST status with null password")
    void createUserPostShouldReturnBadRequestStatusWithNullPassword() {
        UserCreateRequest request = new UserCreateRequest(
                null,
                "user@domain.ext",
                "John",
                "Connor",
                LocalDate.now(),
                "5869995852"
        );
        when(userService.create(request)).thenReturn(ResponseEntity.badRequest().build());

        ResponseEntity<UserCreateResponse> result = userController.createUserPost(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

}
