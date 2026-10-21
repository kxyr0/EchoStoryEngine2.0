package io.github.kxyr0.echoanimation.api.animation.marker;

import io.github.kxyr0.echoanimation.api.graph.GraphIdSyntax;

/**
 * Authored identity of one server-side animation marker.
 */
public record AnimationMarkerId(String value) implements Comparable<AnimationMarkerId> {
    public AnimationMarkerId {
        value = GraphIdSyntax.requireNamespaced(value, "AnimationMarkerId");
    }

    public static AnimationMarkerId parse(String value) {
        return new AnimationMarkerId(value);
    }

    @Override
    public int compareTo(AnimationMarkerId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
