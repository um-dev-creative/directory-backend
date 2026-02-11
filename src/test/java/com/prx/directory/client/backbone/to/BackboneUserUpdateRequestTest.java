package com.prx.directory.client.backbone.to;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BackboneUserUpdateRequestTest {

    @Test
    @DisplayName("BackboneUserUpdateRequest: full record construction and getters")
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
    @DisplayName("BackboneUserUpdateRequest.Contact: equals and hashCode")
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
    @DisplayName("BackboneUserUpdateRequest.ContactType: equals and hashCode")
    void testContactTypeEqualsAndHashCode() {
        UUID contactTypeId = UUID.randomUUID();
        BackboneUserUpdateRequest.ContactType contactType1 = new BackboneUserUpdateRequest.ContactType(contactTypeId);
        BackboneUserUpdateRequest.ContactType contactType2 = new BackboneUserUpdateRequest.ContactType(contactTypeId);
        assertEquals(contactType1, contactType2);
        assertEquals(contactType1.hashCode(), contactType2.hashCode());
    }

    @Test
    @DisplayName("BackboneUserUpdateRequest: toString contains key fields")
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

    @Test
    @DisplayName("BackboneUserUpdateRequest: single-role constructor with role populates roleIds")
    void constructor_singleRole_givenRole_populatesRoleIds() {
        UUID applicationId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        BackboneUserUpdateRequest req = new BackboneUserUpdateRequest(applicationId, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, roleId);

        assertEquals(applicationId, req.application());
        assertEquals(Boolean.TRUE, req.notificationEmail());
        assertEquals(Boolean.FALSE, req.notificationSms());
        assertEquals(Boolean.TRUE, req.privacyDataOutActive());
        // defaulted fields
        assertNull(req.displayName());
        assertNull(req.password());
        assertNull(req.contacts());
        assertEquals(Boolean.TRUE, req.active());

        assertNotNull(req.roleIds(), "roleIds should not be null when a roleId is provided");
        assertEquals(1, req.roleIds().size(), "roleIds should contain exactly one item");
        assertEquals(roleId, req.roleIds().get(0), "roleIds should contain the provided roleId");
    }

    @Test
    @DisplayName("BackboneUserUpdateRequest: single-role constructor with null role returns null roleIds")
    void constructor_singleRole_nullRole_returnsNullRoleIds() {
        UUID applicationId = UUID.randomUUID();

        BackboneUserUpdateRequest req = new BackboneUserUpdateRequest(applicationId, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, (UUID) null);

        assertEquals(applicationId, req.application());
        assertEquals(Boolean.FALSE, req.notificationEmail());
        assertEquals(Boolean.FALSE, req.notificationSms());
        assertEquals(Boolean.FALSE, req.privacyDataOutActive());
        assertNull(req.displayName());
        assertNull(req.password());
        assertNull(req.contacts());
        assertEquals(Boolean.TRUE, req.active(), "active default should be true");

        assertNull(req.roleIds(), "roleIds should be null when roleId is null");
    }

}
