package io.github.kxyr0.echoiteminspection.math;

/** Unit quaternion used by both model rendering and future model-local anchor projection. */
public record QuaternionRotation(double x, double y, double z, double w) {
    public QuaternionRotation {
        double length = Math.hypot(Math.hypot(x, y), Math.hypot(z, w));
        if (!Double.isFinite(length) || length < 1.0e-12) {
            throw new IllegalArgumentException("Quaternion must have finite nonzero length.");
        }
        x /= length;
        y /= length;
        z /= length;
        w /= length;
    }

    public static QuaternionRotation identity() { return new QuaternionRotation(0, 0, 0, 1); }

    public static QuaternionRotation euler(double x, double y, double z) {
        return axisX(x).multiply(axisY(y)).multiply(axisZ(z));
    }

    public static QuaternionRotation axisX(double degrees) {
        double half = Math.toRadians(degrees % 360) / 2;
        return new QuaternionRotation(Math.sin(half), 0, 0, Math.cos(half));
    }

    public static QuaternionRotation axisY(double degrees) {
        double half = Math.toRadians(degrees % 360) / 2;
        return new QuaternionRotation(0, Math.sin(half), 0, Math.cos(half));
    }

    public static QuaternionRotation axisZ(double degrees) {
        double half = Math.toRadians(degrees % 360) / 2;
        return new QuaternionRotation(0, 0, Math.sin(half), Math.cos(half));
    }

    public QuaternionRotation multiply(QuaternionRotation right) {
        return new QuaternionRotation(w * right.x + x * right.w + y * right.z - z * right.y,
                w * right.y - x * right.z + y * right.w + z * right.x,
                w * right.z + x * right.y - y * right.x + z * right.w,
                w * right.w - x * right.x - y * right.y - z * right.z);
    }

    public double rotationX() { return Math.toDegrees(Math.atan2(2 * (w * x - y * z), 1 - 2 * (x * x + y * y))); }

    public double rotationY() { return Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, 2 * (w * y + z * x))))); }

    public double rotationZ() { return Math.toDegrees(Math.atan2(2 * (w * z - x * y), 1 - 2 * (y * y + z * z))); }

    /** Applies the exact render orientation to a model-local point, without a Minecraft dependency. */
    public Point rotate(double px, double py, double pz) {
        double tx = 2 * (y * pz - z * py);
        double ty = 2 * (z * px - x * pz);
        double tz = 2 * (x * py - y * px);
        return new Point(px + w * tx + y * tz - z * ty,
                py + w * ty + z * tx - x * tz, pz + w * tz + x * ty - y * tx);
    }

    public record Point(double x, double y, double z) { }
}
