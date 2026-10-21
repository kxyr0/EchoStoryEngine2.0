package io.github.kxyr0.echowardrobe.forge.api.reload;

import java.util.List;
import java.util.Objects;

/**
 * Immutable public reload summary emitted after accepted catalog publication.
 */
public record WardrobeReloadEvent(long catalogRevision, int wearableDefinitionCount, List<String> changedSources) {
    public WardrobeReloadEvent {
        changedSources = List.copyOf(Objects.requireNonNull(changedSources, "changedSources"));
        if (catalogRevision < 0L || wearableDefinitionCount < 0) {
            throw new IllegalArgumentException("Reload event counts must not be negative.");
        }
    }
}
