package io.github.kxyr0.echoplayersleep.api.snapshot;

import io.github.kxyr0.echoplayersleep.api.world.BlockId;
import io.github.kxyr0.echoplayersleep.api.world.BlockPosition;
import io.github.kxyr0.echoplayersleep.api.world.DimensionId;
import java.util.List;
import java.util.Objects;

public record PlayerSleepBedSnapshot(
        DimensionId dimension,
        BlockPosition canonicalPosition,
        BlockId expectedBlock,
        BlockId actualBlock,
        String providerId,
        String facing,
        List<BlockPosition> safeWakePositions) {
    public PlayerSleepBedSnapshot {
        dimension = Objects.requireNonNull(dimension, "dimension");
        canonicalPosition = Objects.requireNonNull(canonicalPosition, "canonicalPosition");
        expectedBlock = Objects.requireNonNull(expectedBlock, "expectedBlock");
        actualBlock = Objects.requireNonNull(actualBlock, "actualBlock");
        providerId = requireText(providerId, "providerId");
        facing = requireText(facing, "facing");
        safeWakePositions = List.copyOf(Objects.requireNonNull(safeWakePositions, "safeWakePositions"));
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
