package io.github.kxyr0.echothoughtspace.core.canvas;

import io.github.kxyr0.echothoughtspace.api.ThoughtCanvasPosition;

public record CanvasTransform(CanvasCamera camera, CanvasViewport viewport, double scale) {
    public static final double REFERENCE_WIDTH = 1920.0D;
    public static final double REFERENCE_HEIGHT = 1080.0D;

    public CanvasTransform {
        if (camera == null || viewport == null || !Double.isFinite(scale) || scale <= 0.0D) {
            throw new IllegalArgumentException("Invalid canvas transform.");
        }
    }

    public static CanvasTransform fit(CanvasCamera camera, CanvasViewport viewport) {
        double scale = Math.min(viewport.framebufferWidth() / REFERENCE_WIDTH,
                viewport.framebufferHeight() / REFERENCE_HEIGHT) * camera.zoom();
        return new CanvasTransform(camera, viewport, scale);
    }

    public CanvasPoint toPhysical(CanvasPoint canvas) {
        CanvasVector relative = canvas.minus(camera.center());
        return new CanvasPoint(viewport.framebufferWidth() * 0.5D + relative.x() * scale,
                viewport.framebufferHeight() * 0.5D + relative.y() * scale);
    }

    public CanvasPoint toPhysical(ThoughtCanvasPosition canvas) {
        return toPhysical(new CanvasPoint(canvas.x(), canvas.y()));
    }

    public CanvasPoint toCanvas(CanvasPoint physical) {
        return new CanvasPoint(camera.center().x()
                + (physical.x() - viewport.framebufferWidth() * 0.5D) / scale,
                camera.center().y() + (physical.y() - viewport.framebufferHeight() * 0.5D) / scale);
    }
}
