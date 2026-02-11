package com.prx.directory.kafka.to;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class RecipientTest {

    @Test
    @DisplayName("Recipient: accessors, equals, hashCode and toString with values")
    void accessors_equals_hashCode_and_toString() {
        Recipient r1 = new Recipient("Alice", "alice@example.com", "Alicey");
        Recipient r2 = new Recipient("Alice", "alice@example.com", "Alicey");

        // accessors (record components)
        assertEquals("Alice", r1.name());
        assertEquals("alice@example.com", r1.email());
        assertEquals("Alicey", r1.alias());

        // equals & hashCode
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());

        // toString contains fields
        String ts = r1.toString();
        assertTrue(ts.contains("name='Alice'"), "toString should contain name");
        assertTrue(ts.contains("email='alice@example.com'"), "toString should contain email");
        assertTrue(ts.contains("alias='Alicey'"), "toString should contain alias");
    }

    @Test
    @DisplayName("Recipient: null components are handled in toString")
    void nullComponents_areHandledInToString() {
        Recipient r = new Recipient(null, null, null);

        assertNull(r.name());
        assertNull(r.email());
        assertNull(r.alias());

        String ts = r.toString();
        // record toString uses the literal null for null components
        assertTrue(ts.contains("name='null'") || ts.contains("name=null"), "toString should indicate null for name");
        assertTrue(ts.contains("email='null'") || ts.contains("email=null"), "toString should indicate null for email");
        assertTrue(ts.contains("alias='null'") || ts.contains("alias=null"), "toString should indicate null for alias");
    }
}
