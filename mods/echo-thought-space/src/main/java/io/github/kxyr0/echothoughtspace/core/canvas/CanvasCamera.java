package io.github.kxyr0.echothoughtspace.core.canvas;

public final class CanvasCamera {
    private CanvasPoint center = new CanvasPoint(0.0D, 0.0D);
    private double zoom = 1.0D;

    public CanvasPoint center() { return center; }
    public double zoom() { return zoom; }

    public void pan(CanvasVector physicalDelta, CanvasTransform transform) {
        center = center.plus(physicalDelta.scale(-1.0D / transform.scale()));
    }

    public void zoomAt(CanvasPoint physicalAnchor, double targetZoom, CanvasViewport viewport) {
        requireZoom(targetZoom);
        CanvasPoint anchoredCanvasPoint = CanvasTransform.fit(this, viewport).toCanvas(physicalAnchor);
        zoom = targetZoom;
        CanvasPoint movedCanvasPoint = CanvasTransform.fit(this, viewport).toCanvas(physicalAnchor);
        center = center.plus(anchoredCanvasPoint.minus(movedCanvasPoint));
    }

    public void constrainZoom(double minimum, double maximum) {
        requireZoom(minimum);
        requireZoom(maximum);
        if (minimum > maximum) throw new IllegalArgumentException("Minimum zoom exceeds maximum zoom.");
        zoom = Math.max(minimum, Math.min(maximum, zoom));
    }

    public void reset(double initialZoom) {
        requireZoom(initialZoom);
        center = new CanvasPoint(0.0D, 0.0D);
        zoom = initialZoom;
    }

    private static void requireZoom(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException("Zoom must be finite and positive.");
        }
    }
}
