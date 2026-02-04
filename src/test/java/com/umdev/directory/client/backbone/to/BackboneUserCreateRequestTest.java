package com.umdev.directory.client.backbone.to;

import com.umdev.commons.general.pojo.Person;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BackboneUserCreateRequest Record Tests")
class BackboneUserCreateRequestTest {

    @Test
    @DisplayName("Should create BackboneUserCreateRequest with all fields")
    void shouldCreateBackboneUserCreateRequestWithAllFields() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String displayName = "userAlias";
        String password = "password";
        String email = "user@example.com";
        Person person = new Person();
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        BackboneUserCreateRequest request = new BackboneUserCreateRequest(
                id,
                alias,
                password,
                email,
                true,
                person,
                roleId,
                applicationId,
                displayName,
                true,
                true,
                true
        );

        assertEquals(id, request.id());
        assertEquals(alias, request.alias());
        assertEquals(password, request.password());
        assertEquals(email, request.email());
        assertEquals(person, request.person());
        assertEquals(roleId, request.roleId());
        assertEquals(applicationId, request.applicationId());
    }

    @Test
    @DisplayName("Should handle null fields in BackboneUserCreateRequest")
    void shouldHandleNullFields() {
        BackboneUserCreateRequest request = new BackboneUserCreateRequest(
                null, null, null, null, true, null, null, null, null, null, null, null
        );

        assertNull(request.id());
        assertNull(request.alias());
        assertNull(request.password());
        assertNull(request.email());
        assertNull(request.person());
        assertNull(request.roleId());
        assertNull(request.applicationId());
        assertNull(request.displayName());
    }

    @Test
    @DisplayName("Should test equality and hashCode")
    void shouldTestEqualityAndHashCode() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String password = "password";
        String email = "user@example.com";
        Person person = new Person();
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        BackboneUserCreateRequest request1 = new BackboneUserCreateRequest(
                id, alias, password, email, true, person, roleId, applicationId, "DisplayName", true, true, true
        );
        BackboneUserCreateRequest request2 = new BackboneUserCreateRequest(
                id, alias, password, email, true, person, roleId, applicationId, "DisplayName", true, true, true
        );

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    @DisplayName("Should test toString method")
    void shouldTestToString() {
        UUID id = UUID.randomUUID();
        Person person = new Person();
        BackboneUserCreateRequest request = new BackboneUserCreateRequest(
                id, "alias", "password", "email@example.com", true, person, UUID.randomUUID(), UUID.randomUUID(), "DisplayName", true, true, true
        );

        assertTrue(request.toString().contains("alias"));
        assertTrue(request.toString().contains("email@example.com"));
        assertTrue(request.toString().contains(person.toString()));
    }

    @Test
    @DisplayName("Should handle mixed null and non-null fields")
    void shouldHandleMixedNullAndNonNullFields() {
        Person person = new Person();
        BackboneUserCreateRequest request = new BackboneUserCreateRequest(
                UUID.randomUUID(), "alias", "password", null, true, person, UUID.randomUUID(), null, "DisplayName", null, true, null
        );

        assertNotNull(request.id());
        assertNotNull(request.alias());
        assertNotNull(request.password());
        assertNull(request.email());
        assertNotNull(request.person());
        assertNotNull(request.roleId());
        assertNull(request.applicationId());
        assertNotNull(request.displayName());
        assertNull(request.notificationSms());
        assertTrue(request.notificationEmail());
        assertNull(request.privacyDataOutActive());
    }
}
