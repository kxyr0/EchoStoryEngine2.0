package io.github.kxyr0.echonpc.api.rest.command;

import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable exact-bed sleep request.
 */
public record NpcSleepRequest(
        ActorId npcId,
        NpcWorldId worldId,
        BlockPosition bedPosition,
        BlockId expectedBlock,
        Optional<ActorMutationId> mutationId
) {
    public NpcSleepRequest {
        npcId = Objects.requireNonNull(npcId, "npcId");
        worldId = Objects.requireNonNull(worldId, "worldId");
        bedPosition = Objects.requireNonNull(bedPosition, "bedPosition");
        expectedBlock = Objects.requireNonNull(expectedBlock, "expectedBlock");
        mutationId = Objects.requireNonNull(mutationId, "mutationId");
    }

    public NpcSleepRequest(ActorId npcId, NpcWorldId worldId, BlockPosition bedPosition, BlockId expectedBlock) {
        this(npcId, worldId, bedPosition, expectedBlock, Optional.empty());
    }

    public NpcSleepRequest(ActorId npcId, NpcWorldId worldId, BlockPosition bedPosition,
            BlockId expectedBlock, ActorMutationId mutationId) {
        this(npcId, worldId, bedPosition, expectedBlock,
                Optional.of(Objects.requireNonNull(mutationId, "mutationId")));
    }
}
