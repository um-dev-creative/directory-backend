package com.prx.directory.client.mercury;

import com.prx.directory.client.backbone.BackboneManagedClient;
import com.prx.directory.constant.DirectoryMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class MercuryTokensConstructionTest {

    @Test
    @DisplayName("the Spring constructor uses the system clock and reports a missing managed client")
    void publicConstructor() {
        var tokens = new MercuryTokens(mock(BackboneManagedClient.class), mock(MercuryAuthClient.class), "", "", List.of("mercury:message:write"));

        assertThrows(IllegalStateException.class, tokens::token);
    }

    @Test
    @DisplayName("a message code prints as its number")
    void messageCodeToString() {
        var message = DirectoryMessage.values()[0];

        assertEquals(String.valueOf(message.getCode()), message.getCodeToString());
    }
}
