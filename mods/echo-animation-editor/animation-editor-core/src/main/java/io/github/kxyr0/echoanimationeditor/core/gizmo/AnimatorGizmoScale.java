package io.github.kxyr0.echoanimationeditor.core.gizmo;

public final class AnimatorGizmoScale {
    public double worldSizeForPixels(double distance, double verticalFovDegrees, int viewportHeight, int pixels) {
        if (distance <= 0.0D || viewportHeight <= 0 || pixels <= 0) {
            return 0.0D;
        }
        double visibleHeight = 2.0D * distance * Math.tan(Math.toRadians(verticalFovDegrees) / 2.0D);
        return visibleHeight * pixels / viewportHeight;
    }
}
