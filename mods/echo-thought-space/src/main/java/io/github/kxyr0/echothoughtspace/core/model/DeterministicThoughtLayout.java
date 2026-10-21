package io.github.kxyr0.echothoughtspace.core.model;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;

public final class DeterministicThoughtLayout {
    private static final double GOLDEN_ANGLE = Math.PI * (3.0D - Math.sqrt(5.0D));
    private static final double SPACING = 150.0D;

    public CanvasPoint position(int slot) {
        if (slot < 0) throw new IllegalArgumentException("Slot must not be negative.");
        if (slot == 0) return new CanvasPoint(0.0D, 0.0D);
        double radius = SPACING * Math.sqrt(slot);
        double angle = slot * GOLDEN_ANGLE;
        return new CanvasPoint(Math.cos(angle) * radius, Math.sin(angle) * radius);
    }
}

