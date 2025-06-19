package com.prx.directory.api.v1.to;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PutUserRequestTest {

    @Test
    void testRecordConstructionAndGetters() {
        UUID contactId = UUID.randomUUID();
        String contactContent = "1234567890";
        PatchUserRequest.Contact contact = new PatchUserRequest.Contact(contactId, contactContent);

        PatchUserRequest request = new PatchUserRequest(
                null, // password
                "user@example.com",
                "John",
                "M.",
                "Doe",
                "John Doe",
                true,
                false,
                true,
                "Male",
                LocalDate.of(1990, 1, 1),
                contact,
                UUID.randomUUID(),
                true
        );

        assertNull(request.password());
        assertEquals("user@example.com", request.email());
        assertEquals("John", request.firstName());
        assertEquals("M.", request.middleName());
        assertEquals("Doe", request.lastName());
        assertEquals("John Doe", request.displayName());
        assertTrue(request.notificationEmail());
        assertFalse(request.notificationSms());
        assertTrue(request.privacyDataOutActive());
        assertEquals("Male", request.gender());
        assertEquals(LocalDate.of(1990, 1, 1), request.birthdate());
        assertEquals(contact, request.contact());
        assertNotNull(request.roleId());
        assertTrue(request.active());
    }

    @Test
    void testContactEqualsAndHashCode() {
        UUID contactId = UUID.randomUUID();
        PatchUserRequest.Contact contact1 = new PatchUserRequest.Contact(contactId, "1234567890");
        PatchUserRequest.Contact contact2 = new PatchUserRequest.Contact(contactId, "1234567890");
        assertEquals(contact1, contact2);
        assertEquals(contact1.hashCode(), contact2.hashCode());
    }

    @Test
    void testToString() {
        UUID contactId = UUID.randomUUID();
        PatchUserRequest.Contact contact = new PatchUserRequest.Contact(contactId, "1234567890");
        PatchUserRequest request = new PatchUserRequest(
                null, // password
                "user@example.com",
                "John",
                "M.",
                "Doe",
                "John Doe",
                true,
                false,
                true,
                "Male",
                LocalDate.of(1990, 1, 1),
                contact,
                UUID.randomUUID(),
                true
        );
        assertTrue(request.toString().contains("user@example.com"));
        assertTrue(contact.toString().contains("1234567890"));
    }

    @Test
    void testNullContact() {
        PatchUserRequest request = new PatchUserRequest(
                null, // password
                "user@example.com",
                "John",
                "M.",
                "Doe",
                "John Doe",
                null, // notificationEmail
                null, // notificationSms
                null, // privacyDataOutActive
                null, // gender
                null, // birthdate
                null, // contact
                null, // roleId
                null  // active
        );
        assertNull(request.contact());
        assertNull(request.roleId());
        assertNull(request.active());
        assertNull(request.notificationEmail());
        assertNull(request.notificationSms());
        assertNull(request.privacyDataOutActive());
        assertNull(request.gender());
        assertNull(request.birthdate());
    }

    @Test
    void testContactNotEquals() {
        UUID contactId1 = UUID.randomUUID();
        UUID contactId2 = UUID.randomUUID();
        PatchUserRequest.Contact contact1 = new PatchUserRequest.Contact(contactId1, "1234567890");
        PatchUserRequest.Contact contact2 = new PatchUserRequest.Contact(contactId2, "0987654321");
        assertNotEquals(contact1, contact2);
    }

    @Test
    void testPatchUserRequestNotEquals() {
        PatchUserRequest.Contact contact = new PatchUserRequest.Contact(UUID.randomUUID(), "1234567890");
        PatchUserRequest req1 = new PatchUserRequest(
                null, "user1@example.com", "John", "M.", "Doe", "John Doe", true, false, true, "Male", LocalDate.of(1990, 1, 1), contact, UUID.randomUUID(), true);
        PatchUserRequest req2 = new PatchUserRequest(
                null, "user2@example.com", "Jane", "A.", "Smith", "Jane Smith", false, true, false, "Female", LocalDate.of(1992, 2, 2), contact, UUID.randomUUID(), false);
        assertNotEquals(req1, req2);
        assertNotEquals(req1.hashCode(), req2.hashCode());
    }

    @Test
    void testNullFieldsToString() {
        PatchUserRequest request = new PatchUserRequest(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertNotNull(request.toString());
    }
}
