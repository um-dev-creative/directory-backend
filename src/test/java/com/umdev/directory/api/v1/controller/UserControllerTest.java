package com.umdev.directory.api.v1.controller;

import com.umdev.directory.api.v1.service.UserService;
import com.umdev.directory.api.v1.to.UserCreateRequest;
import com.umdev.directory.api.v1.to.UserCreateResponse;
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
        String alias = "jconnor";
        String password = "abc123";
        String email = "user@domain.ext";
        String firstName = "John";
        String lastName = "Connor";
        LocalDate birthDate = LocalDate.now();
        String phone = "5869995852";
        String displayName = "Jconnor";
        Boolean notificationSms = true;
        Boolean notificationEmail = true;
        Boolean privacyDataOutActive = true;
        UserCreateRequest request = new UserCreateRequest(
                password,
                email,
                firstName,
                lastName,
                birthDate,
                phone,
                displayName,
                notificationSms,
                notificationEmail,
                privacyDataOutActive
        );
        UUID id = UUID.randomUUID();
        LocalDateTime createdDate = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();
        boolean active = true;
        UUID personId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UserCreateResponse response = new UserCreateResponse(
                id,
                alias,
                email,
                createdDate,
                lastUpdate,
                active,
                personId,
                roleId,
                applicationId,
                displayName,
                notificationSms,
                notificationEmail,
                privacyDataOutActive
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
        String password = "abc123";
        String email = "user@domain.ext";
        String firstName = "John";
        String lastName = "Connor";
        LocalDate birthDate = LocalDate.now();
        String phone = "5869995852";
        String displayName = "Jconnor";
        Boolean notificationSms = true;
        Boolean notificationEmail = true;
        Boolean privacyDataOutActive = true;
        UserCreateRequest request = new UserCreateRequest(
                password,
                email,
                firstName,
                lastName,
                birthDate,
                phone,
                displayName,
                notificationSms,
                notificationEmail,
                privacyDataOutActive
        );
        when(userService.create(request)).thenReturn(ResponseEntity.badRequest().build());

        ResponseEntity<UserCreateResponse> result = userController.createUserPost(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    @DisplayName("post should return CONFLICT status with existing username")
    void createUserPostShouldReturnConflictStatusWithExistingUsername() {
        String password = "abc123";
        String email = "user@domain.ext";
        String firstName = "John";
        String lastName = "Connor";
        LocalDate birthDate = LocalDate.now();
        String phone = "5869995852";
        String displayName = "Jconnor";
        Boolean notificationSms = true;
        Boolean notificationEmail = true;
        Boolean privacyDataOutActive = true;
        UserCreateRequest request = new UserCreateRequest(
                password,
                email,
                firstName,
                lastName,
                birthDate,
                phone,
                displayName,
                notificationSms,
                notificationEmail,
                privacyDataOutActive
        );
        when(userService.create(request)).thenReturn(ResponseEntity.status(HttpStatus.CONFLICT).build());

        ResponseEntity<UserCreateResponse> result = userController.createUserPost(request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
    }

    @Test
    @DisplayName("post should return BAD_REQUEST status with null password")
    void createUserPostShouldReturnBadRequestStatusWithNullPassword() {
        String email = "user@domain.ext";
        String firstName = "John";
        String lastName = "Connor";
        LocalDate birthDate = LocalDate.now();
        String phone = "5869995852";
        String displayName = "Jconnor";
        Boolean notificationSms = true;
        Boolean notificationEmail = true;
        Boolean privacyDataOutActive = true;
        UserCreateRequest request = new UserCreateRequest(
                null,
                email,
                firstName,
                lastName,
                birthDate,
                phone,
                displayName,
                notificationSms,
                notificationEmail,
                privacyDataOutActive
        );

        when(userService.create(request)).thenReturn(ResponseEntity.badRequest().build());

        ResponseEntity<UserCreateResponse> result = userController.createUserPost(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

}
