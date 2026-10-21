package io.github.kxyr0.echonpcanimationadapter;

import io.github.kxyr0.echoanimation.api.graph.GraphIdSyntax;

/**
 * Stable authored NPC-animation binding identity.
 */
public record NpcAnimationBindingId(String value) implements Comparable<NpcAnimationBindingId> {
    public NpcAnimationBindingId {
        value = GraphIdSyntax.requireNamespaced(value, "NpcAnimationBindingId");
    }

    public static NpcAnimationBindingId parse(String value) {
        return new NpcAnimationBindingId(value);
    }

    @Override
    public int compareTo(NpcAnimationBindingId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
