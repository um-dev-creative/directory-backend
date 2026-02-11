package com.prx.directory.client.backbone.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BackboneUserCreateResponse Record Tests")
class BackboneUserCreateResponseTest {
    @Test
    @DisplayName("Should create BackboneUserCreateResponse with all properties")
    void testBackboneUserCreateResponseAllProperties() {
        UUID id = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        String displayName = "Display Name";
        Boolean notificationSms = true;
        Boolean notificationEmail = true;
        Boolean privacyDataOutActive = false;
        BackboneUserCreateResponse resp = new BackboneUserCreateResponse(
            id, "alias1", "mail@x.com", now, now, true, personId, roleId, appId,
            displayName, notificationSms, notificationEmail, privacyDataOutActive
        );
        assertEquals(id, resp.id());
        assertEquals("alias1", resp.alias());
        assertEquals("mail@x.com", resp.email());
        assertEquals(now, resp.createdDate());
        assertEquals(now, resp.lastUpdate());
        assertTrue(resp.active());
        assertEquals(personId, resp.personId());
        assertEquals(roleId, resp.roleId());
        assertEquals(appId, resp.applicationId());
        assertEquals(displayName, resp.displayName());
        assertEquals(notificationSms, resp.notificationSms());
        assertEquals(notificationEmail, resp.notificationEmail());
        assertEquals(privacyDataOutActive, resp.privacyDataOutActive());
    }
}
