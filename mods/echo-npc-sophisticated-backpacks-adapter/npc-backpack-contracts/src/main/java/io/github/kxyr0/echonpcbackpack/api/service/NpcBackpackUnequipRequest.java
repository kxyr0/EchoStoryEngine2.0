package io.github.kxyr0.echonpcbackpack.api.service;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPolicy;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import java.util.Objects;

public record NpcBackpackUnequipRequest(ActorId actorId, BackpackBlockLocation destination,
        NpcBackpackCommandId commandId, NpcBackpackTransitionPolicy policy) {
    public NpcBackpackUnequipRequest {
        actorId = Objects.requireNonNull(actorId, "actorId");
        destination = Objects.requireNonNull(destination, "destination");
        commandId = Objects.requireNonNull(commandId, "commandId");
        policy = Objects.requireNonNull(policy, "policy");
    }
}
