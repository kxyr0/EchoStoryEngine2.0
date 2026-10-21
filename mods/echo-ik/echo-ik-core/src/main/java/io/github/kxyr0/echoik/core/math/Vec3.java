package io.github.kxyr0.echoik.core.math;

/**
 * Immutable 3D vector used by platform-neutral IK math.
 */
public record Vec3(double x, double y, double z) {
    private static final double EPSILON = 1.0E-9D;

    public Vec3 add(Vec3 other) {
        return new Vec3(x + other.x, y + other.y, z + other.z);
    }

    public Vec3 subtract(Vec3 other) {
        return new Vec3(x - other.x, y - other.y, z - other.z);
    }

    public Vec3 multiply(double scalar) {
        requireFinite(scalar, "scalar");
        return new Vec3(x * scalar, y * scalar, z * scalar);
    }

    public double dot(Vec3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vec3 cross(Vec3 other) {
        return new Vec3(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x);
    }

    public double length() {
        return Math.sqrt(dot(this));
    }

    public double distanceTo(Vec3 other) {
        return subtract(other).length();
    }

    public Vec3 normalize() {
        double length = length();
        return length <= EPSILON ? new Vec3(0.0D, 0.0D, 0.0D) : multiply(1.0D / length);
    }

    public Vec3 lerp(Vec3 target, double amount) {
        double t = clamp01(amount);
        return new Vec3(
                x + (target.x - x) * t,
                y + (target.y - y) * t,
                z + (target.z - z) * t);
    }

    public boolean nearZero() {
        return length() <= EPSILON;
    }

    public boolean isFinite() {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }

    public static Vec3 zero() {
        return new Vec3(0.0D, 0.0D, 0.0D);
    }

    public static Vec3 unitX() {
        return new Vec3(1.0D, 0.0D, 0.0D);
    }

    public static Vec3 unitY() {
        return new Vec3(0.0D, 1.0D, 0.0D);
    }

    public static Vec3 unitZ() {
        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static double clamp01(double value) {
        requireFinite(value, "amount");
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}
