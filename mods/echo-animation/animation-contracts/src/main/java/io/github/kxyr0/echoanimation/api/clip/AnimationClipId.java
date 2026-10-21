package io.github.kxyr0.echoanimation.api.clip;

import io.github.kxyr0.echoanimation.api.graph.GraphIdSyntax;

/**
 * Stable authored visual clip identity.
 */
public record AnimationClipId(String value) implements Comparable<AnimationClipId> {
    public AnimationClipId {
        value = GraphIdSyntax.requireNamespaced(value, "AnimationClipId");
    }

    public static AnimationClipId parse(String value) {
        return new AnimationClipId(value);
    }

    @Override
    public int compareTo(AnimationClipId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
