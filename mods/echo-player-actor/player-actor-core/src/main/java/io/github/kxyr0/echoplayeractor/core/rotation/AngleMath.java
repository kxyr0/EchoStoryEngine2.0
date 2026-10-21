package io.github.kxyr0.echoplayeractor.core.rotation;

public final class AngleMath {
    private AngleMath() {
    }

    public static double wrapDegrees(double value) {
        double wrapped = value % 360.0D;
        if (wrapped >= 180.0D) {
            wrapped -= 360.0D;
        }
        if (wrapped < -180.0D) {
            wrapped += 360.0D;
        }
        return wrapped;
    }

    public static double shortestDelta(double from, double to) {
        return wrapDegrees(to - from);
    }

    public static double clampPitch(double pitch) {
        return Math.max(-90.0D, Math.min(90.0D, pitch));
    }
}
