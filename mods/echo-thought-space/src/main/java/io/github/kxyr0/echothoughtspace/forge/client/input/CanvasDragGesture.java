package io.github.kxyr0.echothoughtspace.forge.client.input;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import io.github.kxyr0.echothoughtspace.core.canvas.CanvasVector;
import java.util.Optional;

public final class CanvasDragGesture {
    private static final double DRAG_THRESHOLD_SQUARED = 36.0D;
    private CanvasPoint origin;
    private CanvasPoint previous;
    private boolean dragged;

    public void begin(CanvasPoint pointer) {
        origin = pointer;
        previous = pointer;
        dragged = false;
    }

    public Optional<CanvasVector> move(CanvasPoint pointer) {
        if (previous == null) return Optional.empty();
        CanvasVector total = pointer.minus(origin);
        if (total.x() * total.x() + total.y() * total.y() >= DRAG_THRESHOLD_SQUARED) dragged = true;
        CanvasVector delta = pointer.minus(previous);
        previous = pointer;
        return dragged ? Optional.of(delta) : Optional.empty();
    }

    public boolean finish() {
        boolean click = origin != null && !dragged;
        origin = null;
        previous = null;
        dragged = false;
        return click;
    }

    public boolean active() { return origin != null; }
}

