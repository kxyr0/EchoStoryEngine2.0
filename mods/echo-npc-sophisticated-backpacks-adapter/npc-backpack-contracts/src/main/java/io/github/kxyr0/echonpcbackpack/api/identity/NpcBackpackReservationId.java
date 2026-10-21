package io.github.kxyr0.echonpcbackpack.api.identity;

public record NpcBackpackReservationId(String value) implements Comparable<NpcBackpackReservationId> {
    public NpcBackpackReservationId {
        value = BackpackIdSyntax.namespaced(value, "NpcBackpackReservationId");
    }

    @Override
    public int compareTo(NpcBackpackReservationId other) {
        return value.compareTo(other.value);
    }
}
