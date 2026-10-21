package io.github.kxyr0.echonpcbackpack.api.service;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPolicy;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import java.util.Objects;

public record NpcBackpackEquipRequest(ActorId actorId, BackpackBlockLocation source,
        NpcBackpackCommandId commandId, NpcBackpackTransitionPolicy policy) {
    public NpcBackpackEquipRequest {
        actorId = Objects.requireNonNull(actorId, "actorId");
        source = Objects.requireNonNull(source, "source");
        commandId = Objects.requireNonNull(commandId, "commandId");
        policy = Objects.requireNonNull(policy, "policy");
    }
}
