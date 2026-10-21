package io.github.kxyr0.echoanimation.api.graph;

/**
 * Authored state identity inside one animation graph.
 */
public record AnimationStateId(String value) implements Comparable<AnimationStateId> {
    public AnimationStateId {
        value = GraphIdSyntax.requirePath(value, "AnimationStateId");
    }

    @Override
    public int compareTo(AnimationStateId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
