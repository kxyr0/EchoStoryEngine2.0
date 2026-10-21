package io.github.kxyr0.echoanimation.api.graph;

/**
 * Logical caller that owns a channel lease.
 */
public record AnimationOwnerId(String value) implements Comparable<AnimationOwnerId> {
    public AnimationOwnerId {
        value = GraphIdSyntax.requireNamespaced(value, "AnimationOwnerId");
    }

    @Override
    public int compareTo(AnimationOwnerId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
