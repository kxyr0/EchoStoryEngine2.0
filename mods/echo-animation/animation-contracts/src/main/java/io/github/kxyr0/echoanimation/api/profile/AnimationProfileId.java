package io.github.kxyr0.echoanimation.api.profile;

import io.github.kxyr0.echoanimation.api.graph.GraphIdSyntax;

/**
 * Stable authored animation profile identity.
 */
public record AnimationProfileId(String value) implements Comparable<AnimationProfileId> {
    public AnimationProfileId {
        value = GraphIdSyntax.requireNamespaced(value, "AnimationProfileId");
    }

    public static AnimationProfileId parse(String value) {
        return new AnimationProfileId(value);
    }

    @Override
    public int compareTo(AnimationProfileId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
