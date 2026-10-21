package io.github.kxyr0.echotrigger.api.geometry;

import java.util.Objects;

public record SphereArea(Vector3 center, double radius) implements AreaShape {
    public SphereArea {
        center = Objects.requireNonNull(center, "center");
        if (!Double.isFinite(radius) || radius <= 0.0D) {
            throw new IllegalArgumentException("SphereArea radius must be finite and positive.");
        }
    }

    @Override
    public AreaShapeKind kind() {
        return AreaShapeKind.SPHERE;
    }

    @Override
    public Vector3 min() {
        return new Vector3(center.X() - radius, center.Y() - radius, center.Z() - radius);
    }

    @Override
    public Vector3 max() {
        return new Vector3(center.X() + radius, center.Y() + radius, center.Z() + radius);
    }

    @Override
    public boolean contains(Vector3 point) {
        double dx = point.X() - center.X();
        double dy = point.Y() - center.Y();
        double dz = point.Z() - center.Z();
        return dx * dx + dy * dy + dz * dz <= radius * radius + 0.0000001D;
    }
}
