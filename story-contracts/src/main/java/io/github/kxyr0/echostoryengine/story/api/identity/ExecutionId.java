package io.github.kxyr0.echostoryengine.story.api.identity;

import java.util.Objects;

/**
 * Stable identifier for one execution of a story.
 *
 * <p>The identifier is supplied by an adapter or caller and must remain stable across save/load boundaries.
 */
public record ExecutionId(NamespacedId value) {
    public ExecutionId {
        value = Objects.requireNonNull(value, "value");
    }

    /**
     * Parses an execution id in `namespace:path` form.
     */
    public static ExecutionId of(String raw) {
        return new ExecutionId(NamespacedId.of(raw));
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
