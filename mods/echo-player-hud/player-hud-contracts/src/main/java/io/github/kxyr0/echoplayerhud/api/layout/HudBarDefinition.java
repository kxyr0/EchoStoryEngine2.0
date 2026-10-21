package io.github.kxyr0.echoplayerhud.api.layout;

import io.github.kxyr0.echoplayerhud.api.gradient.HudGradient;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricId;
import java.util.Objects;

/**
 * Immutable authored HUD bar definition.
 */
public record HudBarDefinition(
        HudBarId barId,
        HudMetricId metricId,
        HudAnchor anchor,
        HudOrientation orientation,
        HudFillDirection fillDirection,
        int offsetX,
        int offsetY,
        int width,
        int height,
        HudGradient gradient,
        int gradientSteps,
        int backgroundColor,
        boolean enabled,
        boolean drawWhenUnavailable,
        String icon,
        int iconWidth,
        int iconHeight,
        int iconSpacing,
        double iconOffsetX,
        double iconOffsetY,
        int borderWidth,
        int borderColor
) {
    public HudBarDefinition(HudBarId barId, HudMetricId metricId, HudAnchor anchor,
                            HudOrientation orientation, HudFillDirection fillDirection,
                            int offsetX, int offsetY, int width, int height, HudGradient gradient) {
        this(barId, metricId, anchor, orientation, fillDirection, offsetX, offsetY, width, height, gradient,
                1, 0x99000000, true, true, "", height, height, 4, 0, 0, 0, 0);
    }

    public HudBarDefinition(HudBarId barId, HudMetricId metricId, HudAnchor anchor,
                            HudOrientation orientation, HudFillDirection fillDirection,
                            int offsetX, int offsetY, int width, int height, HudGradient gradient,
                            int backgroundColor, boolean enabled) {
        this(barId, metricId, anchor, orientation, fillDirection, offsetX, offsetY, width, height, gradient,
                1, backgroundColor, enabled, true, "", height, height, 4, 0, 0, 0, 0);
    }

    public HudBarDefinition(HudBarId barId, HudMetricId metricId, HudAnchor anchor,
                            HudOrientation orientation, HudFillDirection fillDirection,
                            int offsetX, int offsetY, int width, int height, HudGradient gradient,
                            int backgroundColor, boolean enabled, boolean drawWhenUnavailable) {
        this(barId, metricId, anchor, orientation, fillDirection, offsetX, offsetY, width, height, gradient,
                1, backgroundColor, enabled, drawWhenUnavailable, "", height, height, 4, 0, 0, 0, 0);
    }

    public HudBarDefinition {
        barId = Objects.requireNonNull(barId, "barId");
        metricId = Objects.requireNonNull(metricId, "metricId");
        anchor = Objects.requireNonNull(anchor, "anchor");
        orientation = Objects.requireNonNull(orientation, "orientation");
        fillDirection = Objects.requireNonNull(fillDirection, "fillDirection");
        gradient = Objects.requireNonNull(gradient, "gradient");
        icon = Objects.requireNonNull(icon, "icon").strip();
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("HUD bar size must be positive.");
        }
        if (gradientSteps <= 0) {
            throw new IllegalArgumentException("HUD gradientSteps must be positive.");
        }
        if (iconWidth < 0 || iconHeight < 0 || borderWidth < 0) {
            throw new IllegalArgumentException("HUD icon and border sizes cannot be negative.");
        }
    }
}
