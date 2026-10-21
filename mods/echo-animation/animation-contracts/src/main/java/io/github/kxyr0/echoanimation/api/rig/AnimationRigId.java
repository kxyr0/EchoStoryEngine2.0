package io.github.kxyr0.echoanimation.api.rig;

import io.github.kxyr0.echoanimation.api.graph.GraphIdSyntax;

/**
 * Stable authored animation rig identity.
 */
public record AnimationRigId(String value) implements Comparable<AnimationRigId> {
    public AnimationRigId {
        value = GraphIdSyntax.requireNamespaced(value, "AnimationRigId");
    }

    public static AnimationRigId parse(String value) {
        return new AnimationRigId(value);
    }

    @Override
    public int compareTo(AnimationRigId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
