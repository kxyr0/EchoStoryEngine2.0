package io.github.kxyr0.echonpc.api.identity;

/**
 * Source-authored stable compiled NPC template identity.
 */
public record NpcDefinitionId(String value) implements Comparable<NpcDefinitionId> {
    public NpcDefinitionId {
        value = NamespacedIdSyntax.require(value, "NpcDefinitionId");
    }

    @Override
    public int compareTo(NpcDefinitionId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
