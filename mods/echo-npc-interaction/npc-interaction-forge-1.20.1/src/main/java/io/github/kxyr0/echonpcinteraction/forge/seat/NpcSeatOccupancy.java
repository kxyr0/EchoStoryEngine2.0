package io.github.kxyr0.echonpcinteraction.forge.seat;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;
import java.util.Objects;

public record NpcSeatOccupancy(
        String dimensionId,
        BlockPosition targetPosition,
        ActorId npcId,
        NpcInteractionCommandId commandId,
        String anchorId,
        String vehicleId,
        long revision) {
    public NpcSeatOccupancy {
        dimensionId = Objects.requireNonNull(dimensionId, "dimensionId");
        targetPosition = Objects.requireNonNull(targetPosition, "targetPosition");
        npcId = Objects.requireNonNull(npcId, "npcId");
        commandId = Objects.requireNonNull(commandId, "commandId");
        anchorId = Objects.requireNonNull(anchorId, "anchorId");
        vehicleId = Objects.requireNonNull(vehicleId, "vehicleId");
        if (revision < 1L) {
            throw new IllegalArgumentException("revision must be positive.");
        }
    }

    public String id() {
        return dimensionId + "|" + targetPosition.x() + "," + targetPosition.y() + "," + targetPosition.z();
    }
}
