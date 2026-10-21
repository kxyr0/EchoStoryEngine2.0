package io.github.kxyr0.echoanimation.api.animation;

import io.github.kxyr0.echoanimation.api.graph.GraphIdSyntax;

/**
 * Authored high-level animation identity.
 */
public record AnimationId(String value) implements Comparable<AnimationId> {
    public AnimationId {
        value = GraphIdSyntax.requireNamespaced(value, "AnimationId");
    }

    public static AnimationId parse(String value) {
        return new AnimationId(value);
    }

    @Override
    public int compareTo(AnimationId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
