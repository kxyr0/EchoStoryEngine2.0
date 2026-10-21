package io.github.kxyr0.echostoryengine.story.forge.host.execution;

import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CompiledStoryCatalogEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tracks the catalog entry bound to each non-terminal execution.
 */
public final class ActiveStoryDefinitions {
    private final Map<ExecutionId, CompiledStoryCatalogEntry> entries = new HashMap<>();

    public Optional<CompiledStoryCatalogEntry> find(ExecutionId executionId) {
        return Optional.ofNullable(entries.get(executionId));
    }

    public void remember(ExecutionId executionId, CompiledStoryCatalogEntry entry) {
        entries.put(executionId, entry);
    }

    public void remove(ExecutionId executionId) {
        entries.remove(executionId);
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }
}
