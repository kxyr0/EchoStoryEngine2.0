package io.github.kxyr0.echoiteminspection.model;

/** Authored presentation defaults; all rotations use degrees and input uses framebuffer pixels. */
public record ItemInspectionView(
        double scale, double rotationX, double rotationY, double rotationZ,
        double minZoom, double maxZoom, double initialZoom,
        double rotationSensitivity, double zoomSensitivity
) {
    public ItemInspectionView {
        requirePositive(scale, "scale");
        requireFinite(rotationX, "rotationX");
        requireFinite(rotationY, "rotationY");
        requireFinite(rotationZ, "rotationZ");
        requirePositive(minZoom, "minZoom");
        requirePositive(maxZoom, "maxZoom");
        requirePositive(initialZoom, "initialZoom");
        requirePositive(rotationSensitivity, "rotationSensitivity");
        requirePositive(zoomSensitivity, "zoomSensitivity");
        if (minZoom > maxZoom || initialZoom < minZoom || initialZoom > maxZoom) {
            throw new IllegalArgumentException("Zoom must satisfy 0 < min <= initial <= max.");
        }
    }

    public static ItemInspectionView defaults() {
        return new ItemInspectionView(1, 12, -20, 0, 0.65, 2.4, 1, 0.35, 0.12);
    }

    private static void requirePositive(double value, String field) {
        requireFinite(value, field);
        if (value <= 0) throw new IllegalArgumentException(field + " must be positive.");
    }

    private static void requireFinite(double value, String field) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(field + " must be finite.");
    }
}
