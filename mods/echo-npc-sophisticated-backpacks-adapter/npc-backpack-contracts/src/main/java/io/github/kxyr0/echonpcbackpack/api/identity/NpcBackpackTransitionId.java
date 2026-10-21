package io.github.kxyr0.echonpcbackpack.api.identity;

public record NpcBackpackTransitionId(String value) implements Comparable<NpcBackpackTransitionId> {
    public NpcBackpackTransitionId {
        value = BackpackIdSyntax.namespaced(value, "NpcBackpackTransitionId");
    }

    @Override
    public int compareTo(NpcBackpackTransitionId other) {
        return value.compareTo(other.value);
    }
}
