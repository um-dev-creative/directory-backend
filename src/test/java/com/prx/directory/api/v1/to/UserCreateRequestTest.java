package com.prx.directory.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserCreateRequest Record Tests")
class UserCreateRequestTest {
    @Test
    @DisplayName("Should create UserCreateRequest with all properties")
    void testUserCreateRequestAllProperties() {
        String password = "pass123";
        String email = "test@example.com";
        String firstname = "John";
        String lastname = "Doe";
        LocalDate dateOfBirth = LocalDate.of(1990, 1, 1);
        String phoneNumber = "1234567890";
        String displayName = "John D.";
        Boolean notificationSms = true;
        Boolean notificationEmail = true;
        Boolean privacyDataOutActive = false;
        UserCreateRequest req = new UserCreateRequest(
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
        assertEquals(password, req.password());
        assertEquals(email, req.email());
        assertEquals(firstname, req.firstname());
        assertEquals(lastname, req.lastname());
        assertEquals(dateOfBirth, req.dateOfBirth());
        assertEquals(phoneNumber, req.phoneNumber());
        assertEquals(displayName, req.displayName());
        assertEquals(notificationSms, req.notificationSms());
        assertEquals(notificationEmail, req.notificationEmail());
        assertEquals(privacyDataOutActive, req.privacyDataOutActive());
    }
}
