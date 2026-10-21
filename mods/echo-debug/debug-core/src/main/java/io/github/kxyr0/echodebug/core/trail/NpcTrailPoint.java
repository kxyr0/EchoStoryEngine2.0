package io.github.kxyr0.echodebug.core.trail;

public record NpcTrailPoint(double x, double y, double z, long serverTick) {
    public NpcTrailPoint {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        if (serverTick < 0L) {
            throw new IllegalArgumentException("serverTick must not be negative.");
        }
    }

    double distanceTo(NpcTrailPoint other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}
