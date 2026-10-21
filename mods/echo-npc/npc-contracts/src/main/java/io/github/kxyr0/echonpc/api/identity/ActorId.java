package io.github.kxyr0.echonpc.api.identity;

/**
 * Stable logical NPC actor identity, independent from Minecraft entity UUID and numeric entity ID.
 */
public record ActorId(String value) implements Comparable<ActorId> {
    public ActorId {
        value = NamespacedIdSyntax.require(value, "ActorId");
    }

    @Override
    public int compareTo(ActorId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
