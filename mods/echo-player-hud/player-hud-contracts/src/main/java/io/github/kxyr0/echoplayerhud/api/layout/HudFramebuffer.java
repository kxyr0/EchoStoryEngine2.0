package io.github.kxyr0.echoplayerhud.api.layout;

/**
 * Real framebuffer dimensions plus Minecraft GUI scale for debug output.
 */
public record HudFramebuffer(int width, int height, double guiScale) {
    public HudFramebuffer {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Framebuffer width and height must be positive.");
        }
        if (!Double.isFinite(guiScale) || guiScale <= 0.0D) {
            throw new IllegalArgumentException("GUI scale must be positive and finite.");
        }
    }
}
