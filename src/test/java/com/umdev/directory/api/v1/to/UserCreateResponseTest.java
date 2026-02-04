package com.umdev.directory.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserCreateResponse Record Tests")
class UserCreateResponseTest {
    @Test
    @DisplayName("Should create UserCreateResponse with all properties")
    void testUserCreateResponseAllProperties() {
        UUID id = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        String displayName = "Display Name";
        Boolean notificationSms = true;
        Boolean notificationEmail = true;
        Boolean privacyDataOutActive = false;
        UserCreateResponse resp = new UserCreateResponse(
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

    @Test
    @DisplayName("Should handle null fields in UserCreateResponse")
    void shouldHandleNullFields() {
        UserCreateResponse response = new UserCreateResponse(
            null, null, null, null, null, false, null, null, null, null, null, null, null
        );

        assertNull(response.id());
        assertNull(response.alias());
        assertNull(response.email());
        assertNull(response.createdDate());
        assertNull(response.lastUpdate());
        assertNull(response.personId());
        assertNull(response.roleId());
        assertNull(response.applicationId());
        assertNull(response.displayName());
        assertNull(response.notificationSms());
        assertNull(response.notificationEmail());
        assertNull(response.privacyDataOutActive());
    }

    @Test
    @DisplayName("Should test equality and hashCode")
    void shouldTestEqualityAndHashCode() {
        UUID id = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        UserCreateResponse response1 = new UserCreateResponse(
            id, "alias1", "mail@x.com", now, now, true, personId, roleId, appId,
            "Display Name", true, true, false
        );
        UserCreateResponse response2 = new UserCreateResponse(
            id, "alias1", "mail@x.com", now, now, true, personId, roleId, appId,
            "Display Name", true, true, false
        );

        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    @DisplayName("Should test toString method")
    void shouldTestToString() {
        UUID id = UUID.randomUUID();
        UserCreateResponse response = new UserCreateResponse(
            id, "alias1", "mail@x.com", LocalDateTime.now(), LocalDateTime.now(), true, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "Display Name", true, true, false
        );

        assertTrue(response.toString().contains("alias1"));
        assertTrue(response.toString().contains("mail@x.com"));
    }
}
