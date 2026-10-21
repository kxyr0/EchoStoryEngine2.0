package io.github.kxyr0.echoanimationauthoring.api.draft;

public record AuthoringTransform(
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
    public static AuthoringTransform identity() {
        return new AuthoringTransform(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    }
}
