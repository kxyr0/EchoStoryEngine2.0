package io.github.kxyr0.echonpc.api.identity;

/**
 * Namespaced client resource skin identity. The client appends the PNG path convention.
 */
public record NpcSkinId(String value) implements Comparable<NpcSkinId> {
    public NpcSkinId {
        value = NamespacedIdSyntax.require(value, "NpcSkinId");
    }

    @Override
    public int compareTo(NpcSkinId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
