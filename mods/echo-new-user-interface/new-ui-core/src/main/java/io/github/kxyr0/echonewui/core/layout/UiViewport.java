package io.github.kxyr0.echonewui.core.layout;

public record UiViewport(
        int framebufferWidth,
        int framebufferHeight,
        int guiWidth,
        int guiHeight,
        double guiScale,
        long revision) {

    public UiViewport {
        requirePositive(framebufferWidth, "framebufferWidth");
        requirePositive(framebufferHeight, "framebufferHeight");
        requirePositive(guiWidth, "guiWidth");
        requirePositive(guiHeight, "guiHeight");
        if (!Double.isFinite(guiScale) || guiScale <= 0.0D) {
            throw new IllegalArgumentException("guiScale must be finite and positive: " + guiScale);
        }
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must be non-negative: " + revision);
        }
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
    }
}
