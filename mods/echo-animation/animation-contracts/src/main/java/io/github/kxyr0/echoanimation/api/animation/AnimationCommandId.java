package io.github.kxyr0.echoanimation.api.animation;

import io.github.kxyr0.echoanimation.api.graph.GraphIdSyntax;

/**
 * Caller-supplied idempotency key for animation operations.
 */
public record AnimationCommandId(String value) implements Comparable<AnimationCommandId> {
    public AnimationCommandId {
        value = GraphIdSyntax.requireNamespaced(value, "AnimationCommandId");
    }

    public static AnimationCommandId parse(String value) {
        return new AnimationCommandId(value);
    }

    @Override
    public int compareTo(AnimationCommandId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
