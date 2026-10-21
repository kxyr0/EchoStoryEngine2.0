package io.github.kxyr0.echostoryengine.story.forge.host.catalog;

import io.github.kxyr0.echostoryengine.story.api.identity.CompiledStoryFingerprint;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable snapshot каталога, который меняется только atomic replacement.
 */
public record CompiledStoryCatalogSnapshot(
        long revision,
        Instant installedAt,
        Map<StoryId, CompiledStoryCatalogEntry> stories) {
    public CompiledStoryCatalogSnapshot {
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        installedAt = Objects.requireNonNull(installedAt, "installedAt");
        stories = ordered(Objects.requireNonNull(stories, "stories"));
    }

    public static CompiledStoryCatalogSnapshot empty(Instant installedAt) {
        return new CompiledStoryCatalogSnapshot(0, installedAt, Map.of());
    }

    public Optional<CompiledStoryCatalogEntry> find(StoryId storyId) {
        return Optional.ofNullable(stories.get(Objects.requireNonNull(storyId, "storyId")));
    }

    public Optional<CompiledStoryCatalogEntry> find(
            StoryId storyId,
            CompiledStoryFingerprint fingerprint) {
        return find(storyId).filter(entry -> entry.fingerprint().equals(fingerprint));
    }

    public int storyCount() {
        return stories.size();
    }

    private static Map<StoryId, CompiledStoryCatalogEntry> ordered(
            Map<StoryId, CompiledStoryCatalogEntry> source) {
        LinkedHashMap<StoryId, CompiledStoryCatalogEntry> ordered = new LinkedHashMap<>();
        source.entrySet().stream()
                .sorted(Map.Entry.comparingByKey((left, right) -> left.asString().compareTo(right.asString())))
                .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return Map.copyOf(ordered);
    }
}
