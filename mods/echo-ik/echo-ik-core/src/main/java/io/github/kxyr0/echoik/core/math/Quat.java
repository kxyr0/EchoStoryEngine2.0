package io.github.kxyr0.echoik.core.math;

/**
 * Immutable quaternion with only the operations required by IK solvers.
 */
public record Quat(double x, double y, double z, double w) {
    private static final double EPSILON = 1.0E-9D;

    public Quat {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        requireFinite(w, "w");
    }

    public Quat normalized() {
        double length = Math.sqrt(x * x + y * y + z * z + w * w);
        return length <= EPSILON ? identity() : new Quat(x / length, y / length, z / length, w / length);
    }

    public Quat negate() {
        return new Quat(-x, -y, -z, -w);
    }

    public Quat conjugate() {
        return new Quat(-x, -y, -z, w);
    }

    public Quat multiply(Quat other) {
        return new Quat(
                w * other.x + x * other.w + y * other.z - z * other.y,
                w * other.y - x * other.z + y * other.w + z * other.x,
                w * other.z + x * other.y - y * other.x + z * other.w,
                w * other.w - x * other.x - y * other.y - z * other.z);
    }

    public Vec3 rotate(Vec3 vector) {
        Quat value = new Quat(vector.x(), vector.y(), vector.z(), 0.0D);
        Quat result = normalized().multiply(value).multiply(normalized().conjugate());
        return new Vec3(result.x(), result.y(), result.z());
    }

    public double dot(Quat other) {
        return x * other.x + y * other.y + z * other.z + w * other.w;
    }

    public Quat slerp(Quat target, double amount) {
        double t = clamp01(amount);
        Quat end = target.normalized();
        Quat start = normalized();
        double dot = start.dot(end);
        if (dot < 0.0D) {
            end = end.negate();
            dot = -dot;
        }
        if (dot > 0.9995D) {
            return new Quat(
                    start.x + (end.x - start.x) * t,
                    start.y + (end.y - start.y) * t,
                    start.z + (end.z - start.z) * t,
                    start.w + (end.w - start.w) * t).normalized();
        }
        double theta = Math.acos(Math.max(-1.0D, Math.min(1.0D, dot)));
        double sinTheta = Math.sin(theta);
        double left = Math.sin((1.0D - t) * theta) / sinTheta;
        double right = Math.sin(t * theta) / sinTheta;
        return new Quat(
                start.x * left + end.x * right,
                start.y * left + end.y * right,
                start.z * left + end.z * right,
                start.w * left + end.w * right).normalized();
    }

    public static Quat identity() {
        return new Quat(0.0D, 0.0D, 0.0D, 1.0D);
    }

    public static Quat fromUnitVectors(Vec3 from, Vec3 to) {
        Vec3 a = from.normalize();
        Vec3 b = to.normalize();
        if (a.nearZero() || b.nearZero()) {
            return identity();
        }
        double dot = a.dot(b);
        if (dot < -0.999999D) {
            Vec3 axis = Math.abs(a.x()) < 0.8D ? a.cross(Vec3.unitX()) : a.cross(Vec3.unitY());
            Vec3 normalized = axis.normalize();
            return new Quat(normalized.x(), normalized.y(), normalized.z(), 0.0D).normalized();
        }
        Vec3 cross = a.cross(b);
        return new Quat(cross.x(), cross.y(), cross.z(), 1.0D + dot).normalized();
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
