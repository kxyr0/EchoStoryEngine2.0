package io.github.kxyr0.echoobservation.api.model;

import java.util.Objects;

public record ObservationAnchor(
        String dimension,
        int blockX,
        int blockY,
        int blockZ,
        double offsetX,
        double offsetY,
        double offsetZ,
        ObservationFace face) {
    public ObservationAnchor {
        dimension = Objects.requireNonNull(dimension, "dimension");
        face = Objects.requireNonNull(face, "face");
        if (dimension.isBlank() || !unit(offsetX) || !unit(offsetY) || !unit(offsetZ)) {
            throw new IllegalArgumentException("Observation anchor contains invalid dimension or local offset.");
        }
    }

    public double worldX() { return blockX + offsetX; }
    public double worldY() { return blockY + offsetY; }
    public double worldZ() { return blockZ + offsetZ; }

    private static boolean unit(double value) {
        return Double.isFinite(value) && value >= 0.0D && value <= 1.0D;
    }
}
