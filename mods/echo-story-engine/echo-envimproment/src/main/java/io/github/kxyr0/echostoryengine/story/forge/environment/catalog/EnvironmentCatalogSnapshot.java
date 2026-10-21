package io.github.kxyr0.echostoryengine.story.forge.environment.catalog;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable EnvironmentResource catalog visible to new Environment.Set calls.
 */
public record EnvironmentCatalogSnapshot(
        long revision,
        Instant installedAt,
        Map<NamespacedId, EnvironmentCatalogEntry> entries) {
    public EnvironmentCatalogSnapshot {
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        installedAt = Objects.requireNonNull(installedAt, "installedAt");
        entries = Map.copyOf(Objects.requireNonNull(entries, "entries"));
    }

    public static EnvironmentCatalogSnapshot empty(Instant installedAt) {
        return new EnvironmentCatalogSnapshot(0, installedAt, Map.of());
    }

    public Optional<EnvironmentCatalogEntry> find(NamespacedId id) {
        return Optional.ofNullable(entries.get(id));
    }

    public int environmentCount() {
        return entries.size();
    }
}
