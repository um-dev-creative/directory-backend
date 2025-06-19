package com.prx.directory.client.backbone.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("BackboneUserUpdateResponse Record Tests")
class BackboneUserUpdateResponseTest {
    @Test
    @DisplayName("Should create BackboneUserUpdateResponse with all properties")
    void testBackboneUserUpdateResponseAllProperties() {
        UUID id = UUID.randomUUID();
        String alias = "alias1";
        String email = "mail@x.com";
        String displayName = "Display Name";
        LocalDateTime createdDate = LocalDateTime.of(2025, 6, 18, 0, 29, 47);
        LocalDateTime lastUpdate = LocalDateTime.of(2025, 6, 18, 0, 29, 47);
        Boolean notificationEmail = true;
        Boolean notificationSms = true;
        Boolean privacyDataOutActive = false;
        Boolean active = true;
        UUID personId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        BackboneUserUpdateResponse resp = new BackboneUserUpdateResponse(
            id, alias, email, displayName, createdDate, lastUpdate,
            notificationEmail, notificationSms, privacyDataOutActive, active, personId, roleId, applicationId
        );
        assertEquals(id, resp.id());
        assertEquals(alias, resp.alias());
        assertEquals(email, resp.email());
        assertEquals(displayName, resp.displayName());
        assertEquals(createdDate, resp.createdDate());
        assertEquals(lastUpdate, resp.lastUpdate());
        assertEquals(notificationEmail, resp.notificationEmail());
        assertEquals(notificationSms, resp.notificationSms());
        assertEquals(privacyDataOutActive, resp.privacyDataOutActive());
        assertEquals(active, resp.active());
        assertEquals(personId, resp.personId());
        assertEquals(roleId, resp.roleId());
        assertEquals(applicationId, resp.applicationId());
    }
}
