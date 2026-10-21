package io.github.kxyr0.echotimehud.geometry;

import io.github.kxyr0.echotimehud.api.HudAnchor;
import io.github.kxyr0.echotimehud.api.HudConfig;

public final class TimeHudLayout {
    public HudLayoutResult place(HudLayoutInput input) {
        HudConfig config = input.config();
        double scale = Math.min((double) input.framebufferWidth() / config.referenceWidth(),
                (double) input.framebufferHeight() / config.referenceHeight());
        double canvasWidth = config.referenceWidth() * scale;
        double canvasHeight = config.referenceHeight() * scale;
        PhysicalRect framebuffer = new PhysicalRect(0.0, 0.0, input.framebufferWidth(), input.framebufferHeight());
        PhysicalRect canvas = new PhysicalRect((input.framebufferWidth() - canvasWidth) / 2.0,
                (input.framebufferHeight() - canvasHeight) / 2.0, canvasWidth, canvasHeight);
        double margin = Math.min(config.safeMargin() * scale, Math.min(canvasWidth, canvasHeight) / 2.0);
        PhysicalRect safe = new PhysicalRect(canvas.x() + margin, canvas.y() + margin,
                Math.max(0.0, canvas.width() - 2.0 * margin), Math.max(0.0, canvas.height() - 2.0 * margin));
        double width = input.unscaledTextWidth() * config.textScale() * scale;
        double height = input.unscaledTextHeight() * config.textScale() * scale;
        double x = horizontal(config.anchor(), safe, width) + config.offsetX() * scale;
        double y = vertical(config.anchor(), safe, height) + config.offsetY() * scale;
        PhysicalRect raw = new PhysicalRect(x, y, width, height);
        PhysicalRect placed = config.clampToScreen() ? clamp(raw, safe) : raw;
        return new HudLayoutResult(scale, framebuffer, canvas, safe, raw, placed);
    }

    private double horizontal(HudAnchor anchor, PhysicalRect safe, double width) {
        if (anchor.left()) return safe.x();
        if (anchor.right()) return safe.right() - width;
        return safe.x() + (safe.width() - width) / 2.0;
    }

    private double vertical(HudAnchor anchor, PhysicalRect safe, double height) {
        if (anchor.top()) return safe.y();
        if (anchor.bottom()) return safe.bottom() - height;
        return safe.y() + (safe.height() - height) / 2.0;
    }

    private PhysicalRect clamp(PhysicalRect value, PhysicalRect bounds) {
        double maxX = Math.max(bounds.x(), bounds.right() - value.width());
        double maxY = Math.max(bounds.y(), bounds.bottom() - value.height());
        return new PhysicalRect(Math.max(bounds.x(), Math.min(value.x(), maxX)),
                Math.max(bounds.y(), Math.min(value.y(), maxY)), value.width(), value.height());
    }
}

