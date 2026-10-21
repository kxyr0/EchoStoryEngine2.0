package io.github.kxyr0.echodebug.core.selection;

import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import java.util.Objects;

public record TriggerBlockPoint(String dimension, int x, int y, int z) {
    public TriggerBlockPoint {
        dimension = Objects.requireNonNull(dimension, "dimension").trim();
        if (dimension.isEmpty()) {
            throw new IllegalArgumentException("Point dimension is required.");
        }
    }

    public Vector3 lowerCorner() {
        return new Vector3(x, y, z);
    }

    public Vector3 upperExclusiveCorner() {
        return new Vector3(x + 1.0D, y + 1.0D, z + 1.0D);
    }

    public double distanceTo(double checkedX, double checkedY, double checkedZ) {
        double dx = x + 0.5D - checkedX;
        double dy = y + 0.5D - checkedY;
        double dz = z + 0.5D - checkedZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double distanceTo(Vector3 position) {
        return distanceTo(position.X(), position.Y(), position.Z());
    }
}
