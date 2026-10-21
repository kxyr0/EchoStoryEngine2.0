package io.github.kxyr0.echocutscene.api.camera.framing;

public record CameraFraming(double horizontal, double vertical) {
    public static final CameraFraming CENTER = new CameraFraming(0.0D, 0.0D);

    public CameraFraming {
        finite(horizontal, "horizontal");
        finite(vertical, "vertical");
        if (Math.abs(horizontal) > 1.0D || Math.abs(vertical) > 1.0D) {
            throw new IllegalArgumentException("framing values must be in [-1, 1].");
        }
    }

    public CameraFraming addClamped(double horizontalDelta, double verticalDelta) {
        return new CameraFraming(clamp(horizontal + horizontalDelta), clamp(vertical + verticalDelta));
    }

    private static double clamp(double value) {
        return Math.max(-1.0D, Math.min(1.0D, value));
    }

    private static void finite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}
