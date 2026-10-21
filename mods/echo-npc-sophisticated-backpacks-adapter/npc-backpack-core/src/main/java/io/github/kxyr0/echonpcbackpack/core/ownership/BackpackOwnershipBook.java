package io.github.kxyr0.echonpcbackpack.core.ownership;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.payload.NpcBackpackPayloadRef;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class BackpackOwnershipBook {
    private final Map<ActorId, NpcBackpackPayloadRef> byActor = new LinkedHashMap<>();
    private final Map<UUID, ActorId> byContentsUuid = new LinkedHashMap<>();

    public boolean attach(ActorId actor, NpcBackpackPayloadRef payload) {
        ActorId owner = byContentsUuid.get(payload.contentsUuid());
        if (owner != null && !owner.equals(actor)) return false;
        NpcBackpackPayloadRef old = byActor.putIfAbsent(actor, payload);
        if (old != null && !old.equals(payload)) return false;
        byContentsUuid.put(payload.contentsUuid(), actor);
        return true;
    }

    public Optional<NpcBackpackPayloadRef> payload(ActorId actor) {
        return Optional.ofNullable(byActor.get(actor));
    }

    public void detach(ActorId actor) {
        NpcBackpackPayloadRef removed = byActor.remove(actor);
        if (removed != null) byContentsUuid.remove(removed.contentsUuid(), actor);
    }
}
