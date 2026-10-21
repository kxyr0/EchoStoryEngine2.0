package io.github.kxyr0.echonpc.api.identity;

/**
 * Neutral world/dimension identity used by the public API.
 */
public record NpcWorldId(String value) implements Comparable<NpcWorldId> {
    public NpcWorldId {
        value = NamespacedIdSyntax.require(value, "NpcWorldId");
    }

    @Override
    public int compareTo(NpcWorldId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
