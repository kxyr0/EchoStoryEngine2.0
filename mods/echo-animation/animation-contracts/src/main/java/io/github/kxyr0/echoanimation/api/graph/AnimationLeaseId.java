package io.github.kxyr0.echoanimation.api.graph;

/**
 * Opaque channel-control lease identity.
 */
public record AnimationLeaseId(String value) implements Comparable<AnimationLeaseId> {
    public AnimationLeaseId {
        value = GraphIdSyntax.requirePath(value, "AnimationLeaseId");
    }

    @Override
    public int compareTo(AnimationLeaseId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
