package com.umdev.directory.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PutUserRequestTest {

    @Test
    @DisplayName("Create PutUserRequest with all valid data")
    void createPutUserRequestWithAllValidData() {
        String firstName = "John";
        String lastName = "Doe";
        String displayName = "Johnny";
        Boolean notificationEmail = true;
        Boolean notificationSms = false;
        Boolean privacyDataOutActive = true;
        UUID phoneId = UUID.randomUUID();
        String phoneNumber = "+1234567890";
        var roleIds=List.of(UUID.randomUUID());
        Boolean active = true;

        PutUserRequest request = new PutUserRequest(firstName, lastName, displayName, notificationEmail, notificationSms,
                privacyDataOutActive, phoneId, phoneNumber, roleIds, active);

        assertEquals(firstName, request.firstName());
        assertEquals(lastName, request.lastName());
        assertEquals(displayName, request.displayName());
        assertEquals(notificationEmail, request.notificationEmail());
        assertEquals(notificationSms, request.notificationSms());
        assertEquals(privacyDataOutActive, request.privacyDataOutActive());
        assertEquals(phoneId, request.phoneId());
        assertEquals(phoneNumber, request.phoneNumber());
        assertEquals(roleIds, request.roleIds());
        assertEquals(active, request.active());
    }

    @Test
    @DisplayName("Create PutUserRequest with null values")
    void createPutUserRequestWithNullValues() {
        PutUserRequest request = new PutUserRequest(null, null, null, null, null, null, null, null, null, false);
        assertNull(request.firstName());
        assertNull(request.lastName());
        assertNull(request.displayName());
        assertNull(request.notificationEmail());
        assertNull(request.notificationSms());
        assertNull(request.privacyDataOutActive());
        assertNull(request.phoneId());
        assertNull(request.phoneNumber());
        assertNull(request.roleIds());
        assertFalse(request.active());
    }

    @Test
    @DisplayName("Test PutUserRequest toString method")
    void testToString() {
        PutUserRequest request = new PutUserRequest("Jane", "Smith", "Janie", true, false, false,
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), "555-1234",
                List.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174001")), false);
        String str = request.toString();
        assertTrue(str.contains("Jane"));
        assertTrue(str.contains("Smith"));
        assertTrue(str.contains("Janie"));
        assertTrue(str.contains("true"));
        assertTrue(str.contains("false"));
        assertTrue(str.contains("555-1234"));
        assertTrue(str.contains("123e4567-e89b-12d3-a456-426614174000"));
        assertTrue(str.contains("123e4567-e89b-12d3-a456-426614174001"));
    }

    @Test
    @DisplayName("Test PutUserRequest equals and hashCode")
    void testEqualsAndHashCode() {
        UUID phoneId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        var roleIds = List.of(roleId);
        PutUserRequest req1 = new PutUserRequest("A", "B", "C", true, false, true, phoneId, "123", roleIds, true);
        PutUserRequest req2 = new PutUserRequest("A", "B", "C", true, false, true, phoneId, "123", roleIds, true);
        PutUserRequest req3 = new PutUserRequest("X", "Y", "Z", false, true, false, null, null, null, false);
        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
        assertNotEquals(req1, req3);
    }
}

