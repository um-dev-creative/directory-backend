package com.umdc.directory.client.mercury;

import com.umdc.directory.constant.DirectoryMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class MercuryTokensConstructionTest {

    @Test
    @DisplayName("the Spring constructor uses the system clock and reports a missing managed client")
    void publicConstructor() {
        var tokens = new MercuryTokens(mock(com.umdc.directory.client.backbone.BackboneManagedClient.class), mock(MercuryAuthClient.class), "", "", java.util.List.of());

        assertThrows(MercuryUnavailableException.class, tokens::token);
    }

    @Test
    @DisplayName("a message code prints as its number")
    void messageCodeToString() {
        var message = DirectoryMessage.values()[0];

        assertEquals(String.valueOf(message.getCode()), message.getCodeToString());
    }
}
