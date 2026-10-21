package io.github.kxyr0.echoiteminspection.client.render;

/** Presentation dimensions in physical pixels, mapped once into Minecraft's GUI space. */
public record InspectionViewport(int framebufferWidth, int framebufferHeight,
        double guiScale, int guiWidth, int guiHeight) {
    public InspectionViewport {
        if (framebufferWidth <= 0 || framebufferHeight <= 0 || guiWidth <= 0 || guiHeight <= 0
                || !Double.isFinite(guiScale) || guiScale <= 0) {
            throw new IllegalArgumentException("Viewport dimensions and GUI scale must be positive");
        }
    }

    public double centerX() {
        return framebufferWidth / (2.0 * guiScale);
    }

    public double centerY() {
        return framebufferHeight * 0.44 / guiScale;
    }

    public double modelScale() {
        return Math.min(framebufferWidth, framebufferHeight) * 0.40 / guiScale;
    }

    public double modelScale(double authoredScale, double zoom) {
        double maximum = Math.min(framebufferWidth, framebufferHeight) * 4.0 / guiScale;
        return Math.min(maximum, modelScale() * authoredScale * zoom);
    }

    public double toPhysicalDelta(double guiDelta) {
        return guiDelta * guiScale;
    }
}
