package com.prx.directory.client.to;

import com.prx.commons.general.pojo.Person;
import com.prx.directory.client.backbone.to.BackboneUserCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BackboneUserCreateRequestTest {

    @Test
    @DisplayName("Should create BackboneUserCreateRequest with all fields")
    void shouldCreateBackboneUserCreateRequestWithAllFields() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String password = "password";
        String email = "user@example.com";
        Person person = new Person();
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        BackboneUserCreateRequest request = new BackboneUserCreateRequest(id, alias, password, email, person, roleId, applicationId);

        assertEquals(id, request.id());
        assertEquals(alias, request.alias());
        assertEquals(password, request.password());
        assertEquals(email, request.email());
        assertTrue(request.active());
        assertEquals(person, request.person());
        assertEquals(roleId, request.roleId());
        assertEquals(applicationId, request.applicationId());
    }

    @Test
    @DisplayName("Should create BackboneUserCreateRequest with active set to true by default")
    void shouldCreateBackboneUserCreateRequestWithActiveTrueByDefault() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String password = "password";
        String email = "user@example.com";
        Person person = new Person();
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        BackboneUserCreateRequest request = new BackboneUserCreateRequest(id, alias, password, email, person, roleId, applicationId);

        assertTrue(request.active());
    }

    @Test
    @DisplayName("Should create BackboneUserCreateRequest with active set to false")
    void shouldCreateBackboneUserCreateRequestWithActiveFalse() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String password = "password";
        String email = "user@example.com";
        Person person = new Person();
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        BackboneUserCreateRequest request = new BackboneUserCreateRequest(id, alias, password, email, false, person, roleId, applicationId);

        assertFalse(request.active());
    }

}
