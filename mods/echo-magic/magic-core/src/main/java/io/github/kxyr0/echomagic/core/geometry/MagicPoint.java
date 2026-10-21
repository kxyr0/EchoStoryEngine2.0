package io.github.kxyr0.echomagic.core.geometry;

public record MagicPoint(double x, double y, double z) {
    public MagicPoint {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) throw new IllegalArgumentException("Point must be finite.");
    }

    public MagicPoint add(double dx, double dy, double dz) { return new MagicPoint(x + dx, y + dy, z + dz); }
    public double distanceSquared(MagicPoint other) {
        double dx = x - other.x; double dy = y - other.y; double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
}

