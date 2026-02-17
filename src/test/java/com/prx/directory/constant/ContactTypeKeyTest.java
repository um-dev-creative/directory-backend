package com.prx.directory.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ContactTypeKey enum - toString returns name")
class ContactTypeKeyTest {

    @Test
    @DisplayName("toString returns enum name")
    void toStringReturnsName() {
        assertEquals("SCE", ContactTypeKey.SCE.toString());
        assertEquals("EML", ContactTypeKey.EML.toString());
    }
}

