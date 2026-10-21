package io.github.kxyr0.echotrigger.api.geometry;

import java.util.Objects;

public record BoxArea(Vector3 min, Vector3 max) implements AreaShape {
    public BoxArea {
        min = Objects.requireNonNull(min, "min");
        max = Objects.requireNonNull(max, "max");
        if (max.X() <= min.X() || max.Y() <= min.Y() || max.Z() <= min.Z()) {
            throw new IllegalArgumentException("BoxArea max must be greater than min on every axis.");
        }
    }

    @Override
    public AreaShapeKind kind() {
        return AreaShapeKind.BOX;
    }

    @Override
    public Vector3 center() {
        return min.add(max).divide(2.0D);
    }

    @Override
    public boolean contains(Vector3 point) {
        return point.X() >= min.X() && point.X() < max.X()
                && point.Y() >= min.Y() && point.Y() < max.Y()
                && point.Z() >= min.Z() && point.Z() < max.Z();
    }
}
