package com.umdc.directory.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessEventTypeTest {

    @Test
    void parsesAnyCaseAndTrimsSpaces() {
        assertEquals(BusinessEventType.ROUTE, BusinessEventType.parse("route").orElseThrow());
        assertEquals(BusinessEventType.SAVE, BusinessEventType.parse(" Save ").orElseThrow());
        assertEquals(BusinessEventType.CALL, BusinessEventType.parse("CALL").orElseThrow());
        assertEquals(BusinessEventType.WEB, BusinessEventType.parse("web").orElseThrow());
    }

    @Test
    void nullBlankAndUnknownAreEmpty() {
        assertTrue(BusinessEventType.parse(null).isEmpty());
        assertTrue(BusinessEventType.parse("  ").isEmpty());
        assertTrue(BusinessEventType.parse("LIKE").isEmpty());
    }
}
