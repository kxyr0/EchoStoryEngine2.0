package io.github.kxyr0.echonpcbackpack.api.identity;

public record NpcBackpackCommandId(String value) implements Comparable<NpcBackpackCommandId> {
    public NpcBackpackCommandId {
        value = BackpackIdSyntax.namespaced(value, "NpcBackpackCommandId");
    }

    @Override
    public int compareTo(NpcBackpackCommandId other) {
        return value.compareTo(other.value);
    }
}
