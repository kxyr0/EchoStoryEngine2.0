package io.github.kxyr0.echoplayerhud.core.layout;

import io.github.kxyr0.echoplayerhud.api.layout.HudAnchor;
import io.github.kxyr0.echoplayerhud.api.layout.HudBarDefinition;
import io.github.kxyr0.echoplayerhud.api.layout.HudBounds;
import io.github.kxyr0.echoplayerhud.api.layout.HudFramebuffer;

/**
 * Converts authored reference-canvas bars to real framebuffer coordinates.
 */
public final class HudReferenceCanvasTransform {
    private static final double REFERENCE_WIDTH = 1920.0D;
    private static final double REFERENCE_HEIGHT = 1080.0D;

    public HudBounds resolve(HudBarDefinition bar, HudFramebuffer framebuffer) {
        return resolve(bar, framebuffer, (int) REFERENCE_WIDTH, (int) REFERENCE_HEIGHT);
    }

    public HudBounds resolve(HudBarDefinition bar, HudFramebuffer framebuffer, int referenceWidth,
                             int referenceHeight) {
        return resolve(bar, framebuffer, referenceWidth, referenceHeight, true);
    }

    public HudBounds resolve(HudBarDefinition bar, HudFramebuffer framebuffer, int referenceWidth,
                             int referenceHeight, boolean scaleWithResolution) {
        double scale = scaleWithResolution
                ? Math.min(framebuffer.width() / (double) referenceWidth,
                        framebuffer.height() / (double) referenceHeight)
                : 1.0D;
        int width = Math.max(1, snap(bar.width() * scale));
        int height = Math.max(1, snap(bar.height() * scale));
        int offsetX = snap(bar.offsetX() * scale);
        int offsetY = snap(bar.offsetY() * scale);
        int x = x(bar.anchor(), framebuffer.width(), width, offsetX);
        int y = y(bar.anchor(), framebuffer.height(), height, offsetY);
        return new HudBounds(x, y, width, height);
    }

    private int x(HudAnchor anchor, int framebufferWidth, int width, int offsetX) {
        return switch (anchor) {
            case TOP_RIGHT, BOTTOM_RIGHT -> framebufferWidth - width - offsetX;
            case TOP_CENTER, BOTTOM_CENTER, CENTER -> (framebufferWidth - width) / 2 + offsetX;
            case TOP_LEFT, BOTTOM_LEFT -> offsetX;
        };
    }

    private int y(HudAnchor anchor, int framebufferHeight, int height, int offsetY) {
        return switch (anchor) {
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> framebufferHeight - height - offsetY;
            case CENTER -> (framebufferHeight - height) / 2 + offsetY;
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> offsetY;
        };
    }

    private int snap(double value) {
        return (int) Math.round(value);
    }
}
