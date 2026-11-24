package com.umdc.directory.api.v1.service;

import com.umdc.directory.client.backbone.to.ContactType;
import com.umdc.directory.client.backbone.BackboneClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Source of the contact types (EML, WBH, SCE, MEC, Phone, ...) owned by Backbone.
 * <p>
 * Directory never reads the {@code general} schema: the id to name map is loaded through
 * {@link BackboneClient#findAllContactType()} and cached in memory for a configurable TTL
 * ({@code umdc.directory.contact-types.cache-ttl-seconds}, default 600). When Backbone fails the
 * stale cache is served; when nothing was ever cached the result is empty (names resolve to
 * {@code null}) and a warning is logged, so reads never fail with a 500.
 */
@Service
public class ContactTypeProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactTypeProvider.class);

    private static final Snapshot NOTHING = new Snapshot(List.of(), Map.of(), 0L, false);

    private final BackboneClient backboneClient;
    private final long ttlNanos;
    private final Object lock = new Object();
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>(NOTHING);

    public ContactTypeProvider(BackboneClient backboneClient,
                               @Value("${umdc.directory.contact-types.cache-ttl-seconds:600}") long ttlSeconds) {
        this.backboneClient = backboneClient;
        this.ttlNanos = Math.max(0, ttlSeconds) * 1_000_000_000L;
    }

    /**
     * Contact types currently known; never null and never throws.
     *
     * @return the cached list, refreshed from Backbone when the TTL has expired
     */
    public List<ContactType> contactTypes() {
        return current().types();
    }

    /**
     * Map of contact type id to name; never null and never throws.
     *
     * @return id to name map (empty when Backbone is unavailable and nothing is cached)
     */
    public Map<UUID, String> names() {
        return current().names();
    }

    /**
     * Drops the cache so the next read goes to Backbone.
     */
    public void invalidate() {
        snapshot.set(NOTHING);
    }

    private Snapshot current() {
        var seen = snapshot.get();
        if (isFresh(seen)) {
            return seen;
        }
        synchronized (lock) {
            var latest = snapshot.get();
            if (isFresh(latest)) {
                return latest;
            }
            try {
                var loaded = backboneClient.findAllContactType();
                var types = Objects.isNull(loaded) ? List.<ContactType>of() : loaded.stream()
                        .filter(t -> Objects.nonNull(t) && Objects.nonNull(t.id()) && Objects.nonNull(t.name())).toList();
                var names = new HashMap<UUID, String>();
                types.forEach(t -> names.put(t.id(), t.name()));
                var fresh = new Snapshot(types, Map.copyOf(names), System.nanoTime(), true);
                snapshot.set(fresh);
                return fresh;
            } catch (RuntimeException ex) {
                if (latest.loaded()) {
                    LOGGER.warn("Backbone contact types unavailable, serving the stale cache: {}", ex.getMessage());
                } else {
                    LOGGER.warn("Backbone contact types unavailable and nothing cached, contact type names will be empty: {}",
                            ex.getMessage());
                }
                return latest;
            }
        }
    }

    private boolean isFresh(Snapshot candidate) {
        return candidate.loaded() && System.nanoTime() - candidate.loadedAt() < ttlNanos;
    }

    private record Snapshot(List<ContactType> types, Map<UUID, String> names, long loadedAt, boolean loaded) {
    }
}
