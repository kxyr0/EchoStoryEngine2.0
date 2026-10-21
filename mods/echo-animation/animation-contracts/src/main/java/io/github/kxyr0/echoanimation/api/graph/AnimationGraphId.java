package io.github.kxyr0.echoanimation.api.graph;

/**
 * Stable authored animation graph identity.
 */
public record AnimationGraphId(String value) implements Comparable<AnimationGraphId> {
    public AnimationGraphId {
        value = GraphIdSyntax.requireNamespaced(value, "AnimationGraphId");
    }

    @Override
    public int compareTo(AnimationGraphId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
