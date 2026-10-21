package io.github.kxyr0.echoanimationeditor.api.gizmo;

public record AnimatorTransformDelta(
        double translateX,
        double translateY,
        double translateZ,
        double rotateX,
        double rotateY,
        double rotateZ,
        double scaleX,
        double scaleY,
        double scaleZ
) {
    public static AnimatorTransformDelta none() {
        return new AnimatorTransformDelta(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    }
}
