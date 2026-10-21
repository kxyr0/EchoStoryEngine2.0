package io.github.kxyr0.echoplayersleep.api.snapshot;

import io.github.kxyr0.echoplayersleep.api.world.DimensionId;
import io.github.kxyr0.echoplayersleep.api.world.Vector3;
import java.util.Objects;

public record PlayerSleepDreamDestination(
        DimensionId dimension,
        Vector3 position,
        float yaw,
        float pitch) {
    public PlayerSleepDreamDestination {
        dimension = Objects.requireNonNull(dimension, "dimension");
        position = Objects.requireNonNull(position, "position");
    }
}
