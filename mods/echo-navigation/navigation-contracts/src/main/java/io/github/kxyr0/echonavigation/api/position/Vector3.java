package io.github.kxyr0.echonavigation.api.position;

/**
 * Neutral immutable 3D vector used where no shared Echo Platform vector exists.
 */
public record Vector3(double x, double y, double z) {
    public Vector3 {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
    }

    public double horizontalDistanceTo(Vector3 other) {
        double dx = x - other.x;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}
