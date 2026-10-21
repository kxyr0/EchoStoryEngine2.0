package io.github.kxyr0.echoseating.api.world;

public record Vector3(double x, double y, double z) {
    public double distanceSquared(Vector3 other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
