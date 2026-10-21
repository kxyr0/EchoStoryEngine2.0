package io.github.kxyr0.echotrigger.api.geometry;

import java.util.Objects;

public record CylinderArea(Vector3 center, double radius, double minY, double maxY) implements AreaShape {
    public CylinderArea {
        center = Objects.requireNonNull(center, "center");
        require(radius > 0.0D && Double.isFinite(radius), "CylinderArea radius must be finite and positive.");
        require(Double.isFinite(minY) && Double.isFinite(maxY) && maxY > minY,
                "CylinderArea maxY must be greater than minY.");
    }

    @Override
    public AreaShapeKind kind() {
        return AreaShapeKind.CYLINDER;
    }

    @Override
    public Vector3 min() {
        return new Vector3(center.X() - radius, minY, center.Z() - radius);
    }

    @Override
    public Vector3 max() {
        return new Vector3(center.X() + radius, maxY, center.Z() + radius);
    }

    @Override
    public Vector3 center() {
        return new Vector3(center.X(), (minY + maxY) / 2.0D, center.Z());
    }

    @Override
    public boolean contains(Vector3 point) {
        double dx = point.X() - center.X();
        double dz = point.Z() - center.Z();
        return point.Y() >= minY && point.Y() < maxY
                && dx * dx + dz * dz <= radius * radius + 0.0000001D;
    }

    private static void require(boolean valid, String message) {
        if (!valid) {
            throw new IllegalArgumentException(message);
        }
    }
}
