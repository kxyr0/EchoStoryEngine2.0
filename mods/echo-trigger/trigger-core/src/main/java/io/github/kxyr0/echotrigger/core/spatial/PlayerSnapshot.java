package io.github.kxyr0.echotrigger.core.spatial;

import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import java.util.Objects;

public record PlayerSnapshot(
        TriggerOwnerId owner,
        String dimension,
        double x,
        double y,
        double z
) {
    public PlayerSnapshot {
        owner = Objects.requireNonNull(owner, "owner");
        dimension = Objects.requireNonNull(dimension, "dimension");
    }
}
