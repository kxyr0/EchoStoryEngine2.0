package io.github.kxyr0.echoseating.api.anchor;

import io.github.kxyr0.echoseating.api.id.SeatingRevision;
import io.github.kxyr0.echoseating.api.world.BlockPosition;
import io.github.kxyr0.echoseating.api.world.DimensionId;
import io.github.kxyr0.echoseating.api.world.Vector3;
import java.util.Objects;
import java.util.UUID;

public record SeatAnchor(
        DimensionId dimension,
        Vector3 worldPosition,
        BlockPosition blockPosition,
        SeatAnchorSource source,
        SeatAnchorConfidence confidence,
        UUID vehicleUuid,
        String vehicleEntityTypeId,
        long serverTick,
        SeatingRevision revision) {
    public SeatAnchor {
        dimension = Objects.requireNonNull(dimension, "dimension");
        worldPosition = Objects.requireNonNull(worldPosition, "worldPosition");
        blockPosition = Objects.requireNonNull(blockPosition, "blockPosition");
        source = Objects.requireNonNull(source, "source");
        confidence = Objects.requireNonNull(confidence, "confidence");
        vehicleUuid = Objects.requireNonNull(vehicleUuid, "vehicleUuid");
        vehicleEntityTypeId = Objects.requireNonNull(vehicleEntityTypeId, "vehicleEntityTypeId").trim();
        revision = Objects.requireNonNull(revision, "revision");
        if (vehicleEntityTypeId.isEmpty()) {
            throw new IllegalArgumentException("vehicleEntityTypeId must not be blank.");
        }
    }
}
