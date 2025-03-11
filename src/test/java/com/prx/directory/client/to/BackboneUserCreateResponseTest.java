package com.prx.directory.client.to;

import com.prx.directory.client.backbone.to.BackboneUserCreateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackboneUserCreateResponseTest {

    @Test
    @DisplayName("Should create BackboneUserCreateResponse with all fields")
    void shouldCreateBackboneUserCreateResponseWithAllFields() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        LocalDateTime createdDate = LocalDateTime.now();
        LocalDateTime lastUpdate = LocalDateTime.now();
        boolean active = true;
        UUID personId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        BackboneUserCreateResponse response = new BackboneUserCreateResponse(id, alias, email, createdDate, lastUpdate, active, personId, roleId, applicationId);

        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(createdDate, response.createdDate());
        assertEquals(lastUpdate, response.lastUpdate());
        assertTrue(response.active());
        assertEquals(personId, response.personId());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }


}
