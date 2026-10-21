package io.github.kxyr0.echostoryengine.story.api.identity;

import java.util.Objects;

/**
 * Stable identifier for one requested external action execution.
 */
public record ActionExecutionId(NamespacedId value) {
    public ActionExecutionId {
        value = Objects.requireNonNull(value, "value");
    }

    /**
     * Parses an action execution id in `namespace:path` form.
     */
    public static ActionExecutionId of(String raw) {
        return new ActionExecutionId(NamespacedId.of(raw));
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
