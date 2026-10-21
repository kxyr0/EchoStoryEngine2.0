package io.github.kxyr0.echonpcbackpack.core.reservation;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackReservationId;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackReservationView;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BackpackReservationBook {
    private final Map<BackpackBlockLocation, NpcBackpackReservationView> byLocation = new LinkedHashMap<>();

    public Optional<NpcBackpackReservationView> acquire(NpcBackpackReservationId id, ActorId actor,
            BackpackBlockLocation location, long tick) {
        NpcBackpackReservationView existing = byLocation.get(location);
        if (existing != null && !existing.actorId().equals(actor)) {
            return Optional.empty();
        }
        NpcBackpackReservationView reservation = existing == null
                ? new NpcBackpackReservationView(id, actor, location, tick) : existing;
        byLocation.put(location, reservation);
        return Optional.of(reservation);
    }

    public void restore(NpcBackpackReservationView reservation) {
        NpcBackpackReservationView existing = byLocation.putIfAbsent(reservation.location(), reservation);
        if (existing != null && !existing.id().equals(reservation.id())) {
            throw new IllegalStateException("Conflicting durable backpack reservation.");
        }
    }

    public void release(NpcBackpackReservationId id) {
        byLocation.values().removeIf(value -> value.id().equals(id));
    }

    public List<NpcBackpackReservationView> snapshot() {
        return List.copyOf(byLocation.values());
    }
}
