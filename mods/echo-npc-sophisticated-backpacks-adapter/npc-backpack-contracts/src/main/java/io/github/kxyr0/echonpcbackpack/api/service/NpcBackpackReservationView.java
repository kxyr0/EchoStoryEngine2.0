package io.github.kxyr0.echonpcbackpack.api.service;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackReservationId;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import java.util.Objects;

public record NpcBackpackReservationView(NpcBackpackReservationId id, ActorId actorId,
        BackpackBlockLocation location, long acquiredTick) {
    public NpcBackpackReservationView {
        id = Objects.requireNonNull(id, "id");
        actorId = Objects.requireNonNull(actorId, "actorId");
        location = Objects.requireNonNull(location, "location");
    }
}
