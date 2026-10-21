package io.github.kxyr0.echothoughtspace.forge.client.input;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import io.github.kxyr0.echothoughtspace.core.canvas.CanvasVector;
import java.util.Optional;

public final class ThoughtConnectionGesture {
    private static final double DRAG_THRESHOLD_SQUARED = 36.0D;
    private static final long RETRACT_NANOS = 280_000_000L;
    private ThoughtNodeId sourceId;
    private CanvasPoint source;
    private CanvasPoint endpoint;
    private CanvasPoint physicalOrigin;
    private long retractStartedAt;
    private boolean dragged;
    private boolean validTarget;

    public void begin(ThoughtNodeId id, CanvasPoint canvasSource, CanvasPoint physicalPointer) {
        sourceId = id;
        source = canvasSource;
        endpoint = canvasSource;
        physicalOrigin = physicalPointer;
        retractStartedAt = 0L;
        dragged = false;
        validTarget = false;
    }

    public void move(CanvasPoint physicalPointer, CanvasPoint canvasEndpoint, boolean valid) {
        if (!active()) return;
        CanvasVector distance = physicalPointer.minus(physicalOrigin);
        if (distance.x() * distance.x() + distance.y() * distance.y() >= DRAG_THRESHOLD_SQUARED) {
            dragged = true;
        }
        endpoint = canvasEndpoint;
        validTarget = dragged && valid;
    }

    public void release(boolean success, long nowNanos) {
        if (!active()) return;
        if (success || !dragged) reset();
        else {
            physicalOrigin = null;
            validTarget = false;
            retractStartedAt = nowNanos;
        }
    }

    public Optional<ThoughtConnectionVisual> visual(long nowNanos) {
        if (source == null || !dragged) return Optional.empty();
        if (retractStartedAt == 0L) {
            return Optional.of(new ThoughtConnectionVisual(source, endpoint, 1.0D, validTarget));
        }
        double progress = Math.max(0.0D, (nowNanos - retractStartedAt) / (double) RETRACT_NANOS);
        if (progress >= 1.0D) {
            reset();
            return Optional.empty();
        }
        double eased = 1.0D - Math.pow(1.0D - progress, 3.0D);
        CanvasVector remaining = source.minus(endpoint).scale(eased);
        CanvasPoint movingEndpoint = endpoint.plus(remaining);
        return Optional.of(new ThoughtConnectionVisual(source, movingEndpoint,
                Math.pow(1.0D - progress, 2.0D), false));
    }

    public void reset() {
        sourceId = null;
        source = null;
        endpoint = null;
        physicalOrigin = null;
        retractStartedAt = 0L;
        dragged = false;
        validTarget = false;
    }

    public boolean active() { return sourceId != null && retractStartedAt == 0L; }
    public boolean dragged() { return dragged; }
    public ThoughtNodeId sourceId() { return sourceId; }
}
