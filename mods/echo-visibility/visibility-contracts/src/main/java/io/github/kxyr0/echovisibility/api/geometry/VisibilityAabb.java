package io.github.kxyr0.echovisibility.api.geometry;

import java.util.List;
import java.util.Objects;

public record VisibilityAabb(VisibilityVector3 min, VisibilityVector3 max) {
    public VisibilityAabb {
        min = Objects.requireNonNull(min, "min");
        max = Objects.requireNonNull(max, "max");
        if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
            throw new IllegalArgumentException("AABB min must not exceed max.");
        }
    }

    public VisibilityVector3 center() {
        return new VisibilityVector3((min.x() + max.x()) * 0.5D,
                (min.y() + max.y()) * 0.5D, (min.z() + max.z()) * 0.5D);
    }

    public double volume() {
        return (max.x() - min.x()) * (max.y() - min.y()) * (max.z() - min.z());
    }

    public boolean contains(VisibilityVector3 point) {
        return point.x() >= min.x() && point.x() <= max.x()
                && point.y() >= min.y() && point.y() <= max.y()
                && point.z() >= min.z() && point.z() <= max.z();
    }

    public VisibilityAabb inflate(double amount) {
        if (!Double.isFinite(amount) || amount < 0.0D) {
            throw new IllegalArgumentException("AABB padding must be finite and non-negative.");
        }
        return new VisibilityAabb(new VisibilityVector3(min.x() - amount, min.y() - amount, min.z() - amount),
                new VisibilityVector3(max.x() + amount, max.y() + amount, max.z() + amount));
    }

    public List<VisibilityVector3> corners() {
        return List.of(
                new VisibilityVector3(min.x(), min.y(), min.z()),
                new VisibilityVector3(min.x(), min.y(), max.z()),
                new VisibilityVector3(min.x(), max.y(), min.z()),
                new VisibilityVector3(min.x(), max.y(), max.z()),
                new VisibilityVector3(max.x(), min.y(), min.z()),
                new VisibilityVector3(max.x(), min.y(), max.z()),
                new VisibilityVector3(max.x(), max.y(), min.z()),
                new VisibilityVector3(max.x(), max.y(), max.z()));
    }
}
