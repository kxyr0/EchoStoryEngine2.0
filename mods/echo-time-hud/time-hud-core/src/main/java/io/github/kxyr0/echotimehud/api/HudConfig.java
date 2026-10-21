package io.github.kxyr0.echotimehud.api;

import java.util.Objects;

public record HudConfig(String id, boolean enabled, HudAnchor anchor, double offsetX, double offsetY,
                        int referenceWidth, int referenceHeight, double safeMargin, double textScale,
                        String format, boolean showDay, boolean shadow, boolean background, double opacity,
                        boolean respectHideHud, boolean clampToScreen, HudScreenPolicy screenPolicy,
                        long revision) {
    public HudConfig {
        id = Objects.requireNonNull(id, "id");
        anchor = Objects.requireNonNull(anchor, "anchor");
        format = Objects.requireNonNull(format, "format");
        screenPolicy = Objects.requireNonNull(screenPolicy, "screenPolicy");
        if (id.isBlank()) throw new IllegalArgumentException("Id cannot be blank.");
        if (referenceWidth <= 0 || referenceHeight <= 0) throw new IllegalArgumentException("ReferenceCanvas must be positive.");
        if (!Double.isFinite(offsetX) || !Double.isFinite(offsetY)) throw new IllegalArgumentException("Offset must be finite.");
        if (!Double.isFinite(safeMargin) || safeMargin < 0.0) throw new IllegalArgumentException("SafeMargin cannot be negative.");
        if (!Double.isFinite(textScale) || textScale <= 0.0) throw new IllegalArgumentException("TextScale must be positive.");
        if (!Double.isFinite(opacity) || opacity < 0.0 || opacity > 1.0) throw new IllegalArgumentException("Opacity must be 0..1.");
        if (revision < 0L) throw new IllegalArgumentException("Revision cannot be negative.");
    }
}

