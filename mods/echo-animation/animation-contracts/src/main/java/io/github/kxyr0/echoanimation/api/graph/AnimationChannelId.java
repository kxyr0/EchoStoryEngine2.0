package io.github.kxyr0.echoanimation.api.graph;

/**
 * Channel within one animation graph, such as face or body.
 */
public record AnimationChannelId(String value) implements Comparable<AnimationChannelId> {
    public AnimationChannelId {
        value = GraphIdSyntax.requirePath(value, "AnimationChannelId");
    }

    @Override
    public int compareTo(AnimationChannelId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
