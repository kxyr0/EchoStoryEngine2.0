package io.github.kxyr0.echovisibility.core.catalog;

import io.github.kxyr0.echovisibility.api.identity.VisibilityDefinitionRevision;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class VisibilityWatchCatalogState {
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>(new Snapshot(0L, Map.of()));

    public Optional<VisibilityWatchDefinition> find(VisibilityWatchId watchId) {
        return Optional.ofNullable(snapshot.get().definitions().get(watchId));
    }

    public List<VisibilityWatchDefinition> all() {
        return List.copyOf(snapshot.get().definitions().values());
    }

    public long catalogRevision() {
        return snapshot.get().catalogRevision();
    }

    public Snapshot replace(Collection<VisibilityWatchDefinition> candidates) {
        Map<VisibilityWatchId, VisibilityWatchDefinition> next = index(candidates);
        long revision = snapshot.get().catalogRevision() + 1L;
        Snapshot changed = new Snapshot(revision, next);
        snapshot.set(changed);
        return changed;
    }

    public Snapshot replaceKeepingFailures(
            Collection<VisibilityWatchDefinition> candidates,
            Collection<VisibilityWatchId> failedIds) {
        Map<VisibilityWatchId, VisibilityWatchDefinition> next = new LinkedHashMap<>(index(candidates));
        Snapshot previous = snapshot.get();
        for (VisibilityWatchId failedId : failedIds) {
            Optional.ofNullable(previous.definitions().get(failedId))
                    .ifPresent(definition -> next.put(failedId, definition));
        }
        Snapshot changed = new Snapshot(previous.catalogRevision() + 1L, next);
        snapshot.set(changed);
        return changed;
    }

    private static Map<VisibilityWatchId, VisibilityWatchDefinition> index(
            Collection<VisibilityWatchDefinition> definitions) {
        Map<VisibilityWatchId, VisibilityWatchDefinition> indexed = new LinkedHashMap<>();
        definitions.stream().sorted((left, right) -> left.id().compareTo(right.id())).forEach(definition -> {
            if (indexed.putIfAbsent(definition.id(), definition) != null) {
                throw new IllegalArgumentException("Duplicate VisibilityWatchId: " + definition.id().value());
            }
        });
        return Map.copyOf(indexed);
    }

    public record Snapshot(long catalogRevision, Map<VisibilityWatchId, VisibilityWatchDefinition> definitions) {
        public Snapshot {
            if (catalogRevision < 0L) {
                throw new IllegalArgumentException("Catalog revision must not be negative.");
            }
            definitions = Map.copyOf(definitions);
        }

        public VisibilityDefinitionRevision nextDefinitionRevision() {
            long maximum = definitions.values().stream()
                    .mapToLong(value -> value.revision().value()).max().orElse(0L);
            return new VisibilityDefinitionRevision(maximum + 1L);
        }
    }
}
