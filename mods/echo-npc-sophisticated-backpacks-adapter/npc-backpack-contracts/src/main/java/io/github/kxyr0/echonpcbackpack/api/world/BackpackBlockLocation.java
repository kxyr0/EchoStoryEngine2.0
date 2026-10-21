package io.github.kxyr0.echonpcbackpack.api.world;

import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import java.util.Objects;

public record BackpackBlockLocation(DimensionId dimension, BlockPosition position) {
    public BackpackBlockLocation {
        dimension = Objects.requireNonNull(dimension, "dimension");
        position = Objects.requireNonNull(position, "position");
    }

    @Override
    public String toString() {
        return dimension.value() + "@" + position;
    }
}
