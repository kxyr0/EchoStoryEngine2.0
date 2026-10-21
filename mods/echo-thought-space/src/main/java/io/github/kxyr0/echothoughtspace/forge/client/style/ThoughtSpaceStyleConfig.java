package io.github.kxyr0.echothoughtspace.forge.client.style;

public record ThoughtSpaceStyleConfig(
        double zoomInitial,
        double zoomMin,
        double zoomMax,
        double zoomStep,
        double circleRadius,
        double nodeFontSize,
        String titleText,
        double titleSize,
        double titleX,
        double titleY
) {
    private static final ThoughtSpaceStyleConfig DEFAULTS = new ThoughtSpaceStyleConfig(
            1.0D, 0.35D, 3.0D, 0.15D, 30.0D, 1.0D, "", 1.5D, 0.5D, 0.10D);

    public ThoughtSpaceStyleConfig {
        requireRange(zoomInitial, 0.1D, 10.0D, "zoomInitial");
        requireRange(zoomMin, 0.1D, 10.0D, "zoomMin");
        requireRange(zoomMax, 0.1D, 10.0D, "zoomMax");
        requireRange(zoomStep, 0.01D, 2.0D, "zoomStep");
        requireRange(circleRadius, 4.0D, 400.0D, "circleRadius");
        requireRange(nodeFontSize, 0.25D, 8.0D, "nodeFontSize");
        requireRange(titleSize, 0.25D, 8.0D, "titleSize");
        requireRange(titleX, 0.0D, 1.0D, "titleX");
        requireRange(titleY, 0.0D, 1.0D, "titleY");
        if (zoomMin > zoomMax) throw new IllegalArgumentException("zoomMin exceeds zoomMax.");
        if (zoomInitial < zoomMin || zoomInitial > zoomMax) {
            throw new IllegalArgumentException("zoomInitial must be inside zoomMin..zoomMax.");
        }
        if (titleText == null) throw new IllegalArgumentException("titleText must not be null.");
    }

    public static ThoughtSpaceStyleConfig defaults() { return DEFAULTS; }
    public boolean titleVisible() { return !titleText.isBlank(); }

    private static void requireRange(double value, double minimum, double maximum, String name) {
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be within " + minimum + ".." + maximum + '.');
        }
    }
}
