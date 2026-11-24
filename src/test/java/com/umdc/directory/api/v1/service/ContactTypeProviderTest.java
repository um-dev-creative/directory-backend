package com.umdc.directory.api.v1.service;

import com.umdc.directory.client.backbone.BackboneClient;
import com.umdc.directory.client.backbone.to.ContactType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ContactTypeProvider - contact types come from Backbone, cached in memory")
class ContactTypeProviderTest {

    private final BackboneClient backbone = mock(BackboneClient.class);
    private final UUID emlId = UUID.randomUUID();
    private final UUID wbhId = UUID.randomUUID();

    private List<ContactType> types() {
        return List.of(new ContactType(emlId, "EML", "email", true), new ContactType(wbhId, "WBH", "web", true));
    }

    @Test
    @DisplayName("loads the id to name map once and serves it from the cache while the TTL is valid")
    void cachesWithinTtl() {
        when(backbone.findAllContactType()).thenReturn(types());
        var provider = new ContactTypeProvider(backbone, 600);

        assertEquals("EML", provider.names().get(emlId));
        assertEquals("WBH", provider.names().get(wbhId));
        assertEquals(2, provider.contactTypes().size());
        verify(backbone, times(1)).findAllContactType();
    }

    @Test
    @DisplayName("reloads from Backbone when the TTL is zero")
    void reloadsWhenExpired() {
        when(backbone.findAllContactType()).thenReturn(types());
        var provider = new ContactTypeProvider(backbone, 0);

        provider.names();
        provider.names();
        verify(backbone, times(2)).findAllContactType();
    }

    @Test
    @DisplayName("serves the stale cache when Backbone fails after a successful load")
    void servesStaleCacheOnFailure() {
        when(backbone.findAllContactType()).thenReturn(types()).thenThrow(new IllegalStateException("down"));
        var provider = new ContactTypeProvider(backbone, 0);

        assertEquals("EML", provider.names().get(emlId));
        assertEquals("EML", provider.names().get(emlId));
        assertEquals(2, provider.contactTypes().size());
    }

    @Test
    @DisplayName("degrades to empty results (no exception) when nothing is cached and Backbone fails")
    void emptyWhenNothingCachedAndBackboneFails() {
        when(backbone.findAllContactType()).thenThrow(new IllegalStateException("down"));
        var provider = new ContactTypeProvider(backbone, 600);

        assertTrue(provider.names().isEmpty());
        assertTrue(provider.contactTypes().isEmpty());
    }

    @Test
    @DisplayName("ignores null and incomplete entries and a null list")
    void ignoresIncompleteEntries() {
        when(backbone.findAllContactType()).thenReturn(Arrays.asList(null, new ContactType(null, "X", "x", true),
                new ContactType(UUID.randomUUID(), null, "x", true), new ContactType(emlId, "EML", "email", true)));
        var provider = new ContactTypeProvider(backbone, 600);
        assertEquals(1, provider.names().size());

        when(backbone.findAllContactType()).thenReturn(null);
        provider.invalidate();
        assertTrue(provider.names().isEmpty());
    }

    @Test
    @DisplayName("invalidate forces a reload and a negative TTL behaves as zero")
    void invalidateForcesReload() {
        when(backbone.findAllContactType()).thenReturn(types());
        var provider = new ContactTypeProvider(backbone, 600);
        provider.names();
        provider.invalidate();
        provider.names();
        verify(backbone, times(2)).findAllContactType();
        assertEquals(2, new ContactTypeProvider(backbone, -5).names().size());
    }
}
