package io.github.kxyr0.echoseating.api.zone;

import io.github.kxyr0.echoseating.api.world.BlockPosition;
import java.util.Objects;

public record SeatZoneBox(BlockPosition min, BlockPosition max) {
    public SeatZoneBox {
        min = Objects.requireNonNull(min, "min");
        max = Objects.requireNonNull(max, "max");
        if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
            throw new IllegalArgumentException("SeatZoneBox min must be <= max on every axis.");
        }
    }

    public boolean contains(BlockPosition position) {
        return position.x() >= min.x() && position.x() <= max.x()
                && position.y() >= min.y() && position.y() <= max.y()
                && position.z() >= min.z() && position.z() <= max.z();
    }

    public boolean overlaps(SeatZoneBox other) {
        return min.x() <= other.max().x() && max.x() >= other.min().x()
                && min.y() <= other.max().y() && max.y() >= other.min().y()
                && min.z() <= other.max().z() && max.z() >= other.min().z();
    }
}
