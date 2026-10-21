package io.github.kxyr0.echoplayersleep.api.snapshot;

import io.github.kxyr0.echoplayersleep.api.world.BlockPosition;
import io.github.kxyr0.echoplayersleep.api.world.DimensionId;
import io.github.kxyr0.echoplayersleep.api.world.Vector3;
import java.util.Objects;

public record PlayerSleepOriginSnapshot(
        DimensionId dimension,
        Vector3 position,
        float yaw,
        float pitch,
        BlockPosition sleepingPosition,
        int selectedHotbarSlot,
        String respawnSnapshot) {
    public PlayerSleepOriginSnapshot {
        dimension = Objects.requireNonNull(dimension, "dimension");
        position = Objects.requireNonNull(position, "position");
        sleepingPosition = Objects.requireNonNull(sleepingPosition, "sleepingPosition");
        respawnSnapshot = Objects.requireNonNull(respawnSnapshot, "respawnSnapshot");
        if (selectedHotbarSlot < 0 || selectedHotbarSlot > 8) {
            throw new IllegalArgumentException("selectedHotbarSlot must be in [0, 8]");
        }
    }
}
