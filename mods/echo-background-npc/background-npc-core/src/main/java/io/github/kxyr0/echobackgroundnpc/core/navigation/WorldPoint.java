package io.github.kxyr0.echobackgroundnpc.core.navigation;

import java.util.Objects;

public record WorldPoint(String dimension, double x, double y, double z) {
    public WorldPoint {
        dimension = Objects.requireNonNull(dimension, "dimension").trim();
        if (dimension.isEmpty() || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Invalid world point.");
        }
    }

    public double distanceSquared(WorldPoint other) {
        if (!dimension.equals(other.dimension)) return Double.POSITIVE_INFINITY;
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public WorldPoint lerp(WorldPoint other, double alpha) {
        if (!dimension.equals(other.dimension)) throw new IllegalArgumentException("Dimensions differ.");
        double value = Math.max(0.0, Math.min(1.0, alpha));
        return new WorldPoint(dimension, x + (other.x - x) * value, y + (other.y - y) * value,
                z + (other.z - z) * value);
    }
}
