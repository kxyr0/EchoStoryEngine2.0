package io.github.kxyr0.echoplayerhud.api.layout;

/**
 * Pixel-snapped framebuffer bounds.
 */
public record HudBounds(int x, int y, int width, int height) {
    public HudBounds {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("HUD bounds size cannot be negative.");
        }
    }
}
