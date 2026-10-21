package io.github.kxyr0.echoplayerhud.core.render;

import io.github.kxyr0.echoplayerhud.api.gradient.HudGradient;
import io.github.kxyr0.echoplayerhud.api.layout.HudBounds;
import io.github.kxyr0.echoplayerhud.api.layout.HudFillDirection;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits a filled bar rectangle into colored gradient segments.
 */
public final class HudGradientFill {
    private final HudGradientCompiler colors = new HudGradientCompiler();

    public List<Segment> segments(HudBounds fill, HudFillDirection direction, HudGradient gradient, int authoredSteps) {
        int axis = switch (direction) {
            case LEFT_TO_RIGHT, RIGHT_TO_LEFT -> fill.width();
            case TOP_TO_BOTTOM, BOTTOM_TO_TOP -> fill.height();
        };
        if (axis <= 0) {
            return List.of();
        }
        int steps = Math.max(1, Math.min(authoredSteps, axis));
        List<Segment> segments = new ArrayList<>(steps);
        for (int index = 0; index < steps; index++) {
            int start = (int) Math.round(axis * (index / (double) steps));
            int end = (int) Math.round(axis * ((index + 1) / (double) steps));
            int size = end - start;
            if (size <= 0) {
                continue;
            }
            double sample = (index + 0.5D) / (double) steps;
            segments.add(new Segment(bounds(fill, direction, start, end, size), colors.colorAt(gradient, sample)));
        }
        return segments;
    }

    private HudBounds bounds(HudBounds fill, HudFillDirection direction, int start, int end, int size) {
        return switch (direction) {
            case LEFT_TO_RIGHT -> new HudBounds(fill.x() + start, fill.y(), size, fill.height());
            case RIGHT_TO_LEFT -> new HudBounds(fill.x() + fill.width() - end, fill.y(), size, fill.height());
            case TOP_TO_BOTTOM -> new HudBounds(fill.x(), fill.y() + start, fill.width(), size);
            case BOTTOM_TO_TOP -> new HudBounds(fill.x(), fill.y() + fill.height() - end, fill.width(), size);
        };
    }

    public record Segment(HudBounds bounds, int argb) {
    }
}
