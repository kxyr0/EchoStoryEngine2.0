package io.github.kxyr0.echoanimation.api.animation;

import io.github.kxyr0.echoanimation.api.graph.GraphIdSyntax;

/**
 * Opaque identity for one concrete high-level animation run.
 */
public record AnimationPlaybackId(String value) implements Comparable<AnimationPlaybackId> {
    public AnimationPlaybackId {
        value = GraphIdSyntax.requirePath(value, "AnimationPlaybackId");
    }

    public static AnimationPlaybackId parse(String value) {
        return new AnimationPlaybackId(value);
    }

    @Override
    public int compareTo(AnimationPlaybackId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
