package com.prx.directory.client.backbone.to;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BackboneUserUpdateRequestTest {

    @Test
    void testRecordConstructionAndGetters() {
        UUID contactId = UUID.randomUUID();
        UUID contactTypeId = UUID.randomUUID();
        BackboneUserUpdateRequest.ContactType contactType = new BackboneUserUpdateRequest.ContactType(contactTypeId);
        BackboneUserUpdateRequest.Contact contact = new BackboneUserUpdateRequest.Contact(contactId, "1234567890", contactType, true);
        List<BackboneUserUpdateRequest.Contact> contacts = List.of(contact);
        UUID roleId = UUID.randomUUID();
        var roleIds = List.of(roleId);

        BackboneUserUpdateRequest request = new BackboneUserUpdateRequest(
                UUID.randomUUID(),
                "Display Name",
                "password123",
                true,
                false,
                true,
                "John",
                "M.",
                "Doe",
                "Male",
                LocalDate.of(1990, 1, 1),
                true,
                contacts,
                roleIds
        );

        assertEquals("Display Name", request.displayName());
        assertEquals("password123", request.password());
        assertTrue(request.notificationEmail());
        assertFalse(request.notificationSms());
        assertTrue(request.privacyDataOutActive());
        assertEquals("John", request.firstName());
        assertEquals("M.", request.middleName());
        assertEquals("Doe", request.lastName());
        assertEquals("Male", request.gender());
        assertEquals(LocalDate.of(1990, 1, 1), request.birthdate());
        assertTrue(request.active());
        assertEquals(contacts, request.contacts());
    }

    @Test
    void testContactEqualsAndHashCode() {
        UUID contactId = UUID.randomUUID();
        UUID contactTypeId = UUID.randomUUID();
        BackboneUserUpdateRequest.ContactType contactType = new BackboneUserUpdateRequest.ContactType(contactTypeId);
        BackboneUserUpdateRequest.Contact contact1 = new BackboneUserUpdateRequest.Contact(contactId, "1234567890", contactType, true);
        BackboneUserUpdateRequest.Contact contact2 = new BackboneUserUpdateRequest.Contact(contactId, "1234567890", contactType, true);
        assertEquals(contact1, contact2);
        assertEquals(contact1.hashCode(), contact2.hashCode());
    }

    @Test
    void testContactTypeEqualsAndHashCode() {
        UUID contactTypeId = UUID.randomUUID();
        BackboneUserUpdateRequest.ContactType contactType1 = new BackboneUserUpdateRequest.ContactType(contactTypeId);
        BackboneUserUpdateRequest.ContactType contactType2 = new BackboneUserUpdateRequest.ContactType(contactTypeId);
        assertEquals(contactType1, contactType2);
        assertEquals(contactType1.hashCode(), contactType2.hashCode());
    }

    @Test
    void testToString() {
        UUID contactId = UUID.randomUUID();
        UUID contactTypeId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        var roleIds = List.of(roleId);
        BackboneUserUpdateRequest.ContactType contactType = new BackboneUserUpdateRequest.ContactType(contactTypeId);
        BackboneUserUpdateRequest.Contact contact = new BackboneUserUpdateRequest.Contact(contactId, "1234567890", contactType, true);
        BackboneUserUpdateRequest request = new BackboneUserUpdateRequest(
                applicationId,
                "Display Name",
                "password123",
                true,
                false,
                true,
                "John",
                "M.",
                "Doe",
                "Male",
                LocalDate.of(1990, 1, 1),
                true,
                List.of(contact),
                roleIds
        );
        assertTrue(request.toString().contains("Display Name"));
        assertTrue(contact.toString().contains("1234567890"));
        assertTrue(contactType.toString().contains(contactTypeId.toString()));
    }
}

