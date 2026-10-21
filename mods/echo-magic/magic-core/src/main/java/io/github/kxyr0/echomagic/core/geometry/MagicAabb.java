package io.github.kxyr0.echomagic.core.geometry;

public record MagicAabb(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public MagicAabb {
        if (minX > maxX || minY > maxY || minZ > maxZ) throw new IllegalArgumentException("Invalid AABB bounds.");
    }

    public MagicAabb inflate(double radius) {
        return new MagicAabb(minX - radius, minY - radius, minZ - radius,
                maxX + radius, maxY + radius, maxZ + radius);
    }
}

