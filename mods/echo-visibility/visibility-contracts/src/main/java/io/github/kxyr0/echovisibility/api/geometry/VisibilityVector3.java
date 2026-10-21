package io.github.kxyr0.echovisibility.api.geometry;

public record VisibilityVector3(double x, double y, double z) {
    public VisibilityVector3 {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Visibility vector components must be finite.");
        }
    }

    public VisibilityVector3 add(VisibilityVector3 other) {
        return new VisibilityVector3(x + other.x, y + other.y, z + other.z);
    }

    public VisibilityVector3 subtract(VisibilityVector3 other) {
        return new VisibilityVector3(x - other.x, y - other.y, z - other.z);
    }

    public VisibilityVector3 scale(double value) {
        return new VisibilityVector3(x * value, y * value, z * value);
    }

    public double dot(VisibilityVector3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public VisibilityVector3 cross(VisibilityVector3 other) {
        return new VisibilityVector3(y * other.z - z * other.y,
                z * other.x - x * other.z, x * other.y - y * other.x);
    }

    public double lengthSquared() {
        return dot(this);
    }

    public VisibilityVector3 normalized() {
        double length = Math.sqrt(lengthSquared());
        if (!Double.isFinite(length) || length < 1.0E-9D) {
            throw new IllegalArgumentException("Visibility direction must be non-zero.");
        }
        return scale(1.0D / length);
    }
}
