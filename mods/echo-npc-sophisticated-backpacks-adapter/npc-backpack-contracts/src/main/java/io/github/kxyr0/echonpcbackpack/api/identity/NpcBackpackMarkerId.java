package io.github.kxyr0.echonpcbackpack.api.identity;

public record NpcBackpackMarkerId(String value) implements Comparable<NpcBackpackMarkerId> {
    public NpcBackpackMarkerId {
        value = BackpackIdSyntax.namespaced(value, "NpcBackpackMarkerId");
    }

    @Override
    public int compareTo(NpcBackpackMarkerId other) {
        return value.compareTo(other.value);
    }
}
