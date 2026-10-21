package io.github.kxyr0.echostoryengine.story.api.identity;

import java.util.Objects;

/**
 * Stable identifier for a story definition.
 *
 * <p>The value is namespaced and independent from Minecraft registries. It can be persisted, logged, and compared
 * without loading a platform adapter.
 */
public record StoryId(NamespacedId value) {
    public StoryId {
        value = Objects.requireNonNull(value, "value");
    }

    /**
     * Parses a story id in `namespace:path` form.
     */
    public static StoryId of(String raw) {
        return new StoryId(NamespacedId.of(raw));
    }

    /**
     * Returns the canonical public representation.
     */
    public String asString() {
        return value.asString();
    }

    @Override
    public String toString() {
        return asString();
    }
}
