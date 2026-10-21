package io.github.kxyr0.echostorynpcadapter.action;

import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;

public record SleepPayload(
        ActorId actorId,
        NpcWorldId worldId,
        BlockPosition bedPosition,
        BlockId expectedBlock
) {
    public SleepPayload {
        actorId = java.util.Objects.requireNonNull(actorId, "actorId");
        worldId = java.util.Objects.requireNonNull(worldId, "worldId");
        bedPosition = java.util.Objects.requireNonNull(bedPosition, "bedPosition");
        expectedBlock = java.util.Objects.requireNonNull(expectedBlock, "expectedBlock");
    }
}
