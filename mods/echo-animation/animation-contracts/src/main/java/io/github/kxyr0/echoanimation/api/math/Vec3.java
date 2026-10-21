package io.github.kxyr0.echoanimation.api.math;

/**
 * Immutable three-dimensional vector.
 */
public record Vec3(double x, double y, double z) {
    public static final Vec3 ZERO = new Vec3(0.0D, 0.0D, 0.0D);
    public static final Vec3 ONE = new Vec3(1.0D, 1.0D, 1.0D);

    public Vec3 {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
    }

    public Vec3 plus(Vec3 other) {
        return new Vec3(x + other.x, y + other.y, z + other.z);
    }

    public Vec3 minus(Vec3 other) {
        return new Vec3(x - other.x, y - other.y, z - other.z);
    }

    public Vec3 componentTimes(Vec3 other) {
        return new Vec3(x * other.x, y * other.y, z * other.z);
    }

    private static void requireFinite(double value, String field) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Vec3." + field + " должен быть finite.");
        }
    }
}

