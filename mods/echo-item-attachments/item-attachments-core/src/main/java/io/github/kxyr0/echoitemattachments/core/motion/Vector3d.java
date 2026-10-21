package io.github.kxyr0.echoitemattachments.core.motion;

public record Vector3d(double x, double y, double z) {
    public Vector3d {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("vector must be finite");
        }
    }

    public Vector3d subtract(Vector3d other) {
        return new Vector3d(x - other.x, y - other.y, z - other.z);
    }

    public Vector3d scale(double factor) {
        return new Vector3d(x * factor, y * factor, z * factor);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vector3d clamp(double maximum) {
        double length = length();
        return length <= maximum || length == 0.0D ? this : scale(maximum / length);
    }
}
