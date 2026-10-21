package io.github.kxyr0.echotrigger.api.model;

import java.util.Objects;

public record TriggerArea(String dimension, int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {
    public TriggerArea {
        dimension = Objects.requireNonNull(dimension, "dimension").trim();
        if (!dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Area dimension must be namespace:path.");
        }
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            throw new IllegalArgumentException("Area sizes must be positive.");
        }
    }

    public static TriggerArea block(String dimension, int x, int y, int z) {
        return new TriggerArea(dimension, x, y, z, 1, 1, 1);
    }

    public static TriggerArea fromTo(String dimension, TriggerBlockPos first, TriggerBlockPos second) {
        int minX = Math.min(first.x(), second.x());
        int minY = Math.min(first.y(), second.y());
        int minZ = Math.min(first.z(), second.z());
        return new TriggerArea(dimension, minX, minY, minZ,
                Math.abs(second.x() - first.x()) + 1,
                Math.abs(second.y() - first.y()) + 1,
                Math.abs(second.z() - first.z()) + 1);
    }

    public boolean contains(String candidateDimension, double x, double y, double z) {
        return dimension.equals(candidateDimension)
                && x >= minX
                && y >= minY
                && z >= minZ
                && x < minX + sizeX
                && y < minY + sizeY
                && z < minZ + sizeZ;
    }
}
