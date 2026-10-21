package io.github.kxyr0.echoanimationeditor.animationadapter.authoring;

public record EchoAuthoringTransform(
        double x,
        double y,
        double z,
        double rotX,
        double rotY,
        double rotZ,
        double scaleX,
        double scaleY,
        double scaleZ
) {
    public static EchoAuthoringTransform translate(double x, double y, double z) {
        return new EchoAuthoringTransform(x, y, z, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    }

    public static EchoAuthoringTransform rotate(double x, double y, double z) {
        return new EchoAuthoringTransform(0.0D, 0.0D, 0.0D, x, y, z, 1.0D, 1.0D, 1.0D);
    }

    public static EchoAuthoringTransform scale(double value) {
        double clamped = Math.max(0.05D, value);
        return new EchoAuthoringTransform(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, clamped, clamped, clamped);
    }

    public static EchoAuthoringTransform scale(double x, double y, double z) {
        return new EchoAuthoringTransform(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                Math.max(0.05D, x), Math.max(0.05D, y), Math.max(0.05D, z));
    }
}
