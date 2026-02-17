package com.prx.directory.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DirectoryMessage enum - code and status")
class DirectoryMessageTest {

    @Test
    @DisplayName("DATABASE_CERTIFICATE_ERROR has expected code and status")
    void certificateError() {
        DirectoryMessage m = DirectoryMessage.DATABASE_CERTIFICATE_ERROR;
        assertEquals(1001, m.getCode());
        assertEquals("Database Certificate Error", m.getStatus());
    }
}

