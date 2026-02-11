package com.prx.directory.kafka.to;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmailMessageTOTest {

    @Test
    @DisplayName("EmailMessageTO: fields and toString with populated values")
    void fields_and_toString_and_nulls() {
        UUID templateId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Recipient r1 = new Recipient("Bob", "bob@example.com", null);
        Recipient r2 = new Recipient("Carol", "carol@example.com", "Caz");
        LocalDateTime sendDate = LocalDateTime.now();
        Map<String, Object> params = new HashMap<>();
        params.put("k1", "v1");

        EmailMessageTO m = new EmailMessageTO(
                templateId,
                userId,
                "no-reply@example.com",
                List.of(r1),
                List.of(r2),
                "Subject",
                "Body text",
                sendDate,
                params
        );

        assertEquals(templateId, m.templateDefinedId());
        assertEquals(userId, m.userId());
        assertEquals("no-reply@example.com", m.from());
        assertEquals(List.of(r1), m.to());
        assertEquals(List.of(r2), m.cc());
        assertEquals("Subject", m.subject());
        assertEquals("Body text", m.body());
        assertEquals(sendDate, m.sendDate());
        assertEquals(params, m.params());

        String ts = m.toString();
        assertTrue(ts.contains("template_defined_id='" + templateId + "'"));
        assertTrue(ts.contains("user_id='" + userId + "'"));
        assertTrue(ts.contains("from='no-reply@example.com'"));
        assertTrue(ts.contains("to=" + List.of(r1)));
        assertTrue(ts.contains("cc=" + List.of(r2)));
        assertTrue(ts.contains("subject='Subject'"));
        assertTrue(ts.contains("body='Body text'"));
        assertTrue(ts.contains("send_date='" + sendDate + "'"));
        assertTrue(ts.contains("params=" + params));
    }

    @Test
    @DisplayName("EmailMessageTO: null components handled and toString indicates nulls")
    void nullCollections_and_nullParams_and_toStringHandleNulls() {
        EmailMessageTO m = new EmailMessageTO(null, null, null, null, null, null, null, null, null);

        assertNull(m.templateDefinedId());
        assertNull(m.userId());
        assertNull(m.from());
        assertNull(m.to());
        assertNull(m.cc());
        assertNull(m.subject());
        assertNull(m.body());
        assertNull(m.sendDate());
        assertNull(m.params());

        String ts = m.toString();
        assertTrue(ts.contains("template_defined_id='null'") || ts.contains("template_defined_id=null"));
        assertTrue(ts.contains("user_id='null'") || ts.contains("user_id=null"));
        assertTrue(ts.contains("from='null'") || ts.contains("from=null"));
        assertTrue(ts.contains("to=null") || ts.contains("to='null'"));
        assertTrue(ts.contains("cc=null") || ts.contains("cc='null'"));
        assertTrue(ts.contains("subject='null'") || ts.contains("subject=null"));
        assertTrue(ts.contains("body='null'") || ts.contains("body=null"));
        assertTrue(ts.contains("send_date='null'") || ts.contains("send_date=null"));
        assertTrue(ts.contains("params=null") || ts.contains("params='null'"));
    }
}
