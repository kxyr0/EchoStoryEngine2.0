package io.github.kxyr0.echoplayerhud.core.render;

import io.github.kxyr0.echoplayerhud.api.layout.HudBounds;
import io.github.kxyr0.echoplayerhud.api.layout.HudFillDirection;

/**
 * Computes the framebuffer rectangle filled by a metric value.
 */
public final class HudBarFill {
    public HudBounds fill(HudBounds bounds, HudFillDirection direction, double normalized) {
        double clamped = Math.max(0.0D, Math.min(1.0D, normalized));
        int filledWidth = (int) Math.round(bounds.width() * clamped);
        int filledHeight = (int) Math.round(bounds.height() * clamped);
        return switch (direction) {
            case LEFT_TO_RIGHT -> new HudBounds(bounds.x(), bounds.y(), filledWidth, bounds.height());
            case RIGHT_TO_LEFT -> new HudBounds(bounds.x() + bounds.width() - filledWidth, bounds.y(),
                    filledWidth, bounds.height());
            case TOP_TO_BOTTOM -> new HudBounds(bounds.x(), bounds.y(), bounds.width(), filledHeight);
            case BOTTOM_TO_TOP -> new HudBounds(bounds.x(), bounds.y() + bounds.height() - filledHeight,
                    bounds.width(), filledHeight);
        };
    }
}
