package com.umdev.directory.api.v1.service;

import com.umdev.directory.api.v1.to.UserCreateRequest;
import com.umdev.directory.api.v1.to.UserCreateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserServiceTest {

    private final UserService userService = new UserService() {};

    @Test
    @DisplayName("Create User Successfully")
    void createUserSuccessfully() {
        String password = "abc123";
        String email = "user@domain.ext";
        String firstname = "John";
        String lastname = "Connor";
        LocalDate dateOfBirth = LocalDate.now();
        String phoneNumber = "5869995852";
        String displayName = "John C.";
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
        ResponseEntity<UserCreateResponse> response = userService.create(request);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Test
    @DisplayName("Create User with Null Request")
    void createUserWithNullRequest() {
        ResponseEntity<UserCreateResponse> response = userService.create(null);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }
}
