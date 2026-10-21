package io.github.kxyr0.echotimehud.geometry;

import io.github.kxyr0.echotimehud.api.HudConfig;
import java.util.Objects;

public record HudLayoutInput(int framebufferWidth, int framebufferHeight,
                             double unscaledTextWidth, double unscaledTextHeight, HudConfig config) {
    public HudLayoutInput {
        config = Objects.requireNonNull(config, "config");
        if (framebufferWidth <= 0 || framebufferHeight <= 0) throw new IllegalArgumentException("Framebuffer must be positive.");
        if (!Double.isFinite(unscaledTextWidth) || !Double.isFinite(unscaledTextHeight)
                || unscaledTextWidth < 0.0 || unscaledTextHeight < 0.0) {
            throw new IllegalArgumentException("Text bounds must be finite and non-negative.");
        }
    }
}

