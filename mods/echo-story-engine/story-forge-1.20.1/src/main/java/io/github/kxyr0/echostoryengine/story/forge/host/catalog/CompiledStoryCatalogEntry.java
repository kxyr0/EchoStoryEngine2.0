package io.github.kxyr0.echostoryengine.story.forge.host.catalog;

import io.github.kxyr0.echostoryengine.story.api.identity.CompiledStoryFingerprint;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import java.util.Objects;
import java.util.Set;

/**
 * Одно скомпилированное story definition с source identity и action types.
 */
public record CompiledStoryCatalogEntry(
        StoryDefinition definition,
        CompiledStoryFingerprint fingerprint,
        String sourceId,
        Set<NamespacedId> actionTypes) {
    public CompiledStoryCatalogEntry {
        definition = Objects.requireNonNull(definition, "definition");
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        actionTypes = Set.copyOf(Objects.requireNonNull(actionTypes, "actionTypes"));
    }
}
