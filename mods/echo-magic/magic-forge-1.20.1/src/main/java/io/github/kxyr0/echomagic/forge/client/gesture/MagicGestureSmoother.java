package io.github.kxyr0.echomagic.forge.client.gesture;

import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;

final class MagicGestureSmoother {
    private static final double MINIMUM_CUTOFF = 3.8D;
    private static final double DERIVATIVE_CUTOFF = 1.0D;
    private static final double SPEED_RESPONSE = 0.18D;
    private static final double CORNER_COSINE = 0.48D;
    private Axis x;
    private Axis y;
    private MagicGesturePoint previousRaw;
    private double motionX;
    private double motionY;

    Sample filter(MagicGesturePoint point, boolean force) {
        if (previousRaw == null) {
            x = new Axis(point.x()); y = new Axis(point.y()); previousRaw = point;
            return new Sample(point, null);
        }
        double seconds = Math.max(0.001D, (point.elapsedMillis() - previousRaw.elapsedMillis()) / 1000.0D);
        double nextX = point.x() - previousRaw.x(); double nextY = point.y() - previousRaw.y();
        boolean corner = isCorner(nextX, nextY);
        MagicGesturePoint anchor = corner ? previousRaw : null;
        MagicGesturePoint filtered;
        if (force || corner) {
            x.reset(point.x()); y.reset(point.y()); filtered = point;
        } else {
            filtered = new MagicGesturePoint(x.filter(point.x(), seconds), y.filter(point.y(), seconds),
                    point.elapsedMillis());
        }
        if (Math.hypot(nextX, nextY) >= 0.0025D) { motionX = nextX; motionY = nextY; }
        previousRaw = point;
        return new Sample(filtered, anchor);
    }

    private boolean isCorner(double nextX, double nextY) {
        double previousLength = Math.hypot(motionX, motionY); double nextLength = Math.hypot(nextX, nextY);
        if (previousLength < 0.0025D || nextLength < 0.0025D) return false;
        double cosine = (motionX * nextX + motionY * nextY) / (previousLength * nextLength);
        return cosine < CORNER_COSINE;
    }

    private static double alpha(double cutoff, double seconds) {
        double timeConstant = 1.0D / (Math.PI * 2.0D * cutoff);
        return 1.0D / (1.0D + timeConstant / seconds);
    }

    record Sample(MagicGesturePoint point, MagicGesturePoint corner) { }

    private static final class Axis {
        private double raw;
        private double filtered;
        private double derivative;
        private Axis(double initial) { reset(initial); }
        private void reset(double value) { raw = value; filtered = value; derivative = 0.0D; }
        private double filter(double value, double seconds) {
            double speed = (value - raw) / seconds; raw = value;
            double derivativeAlpha = alpha(DERIVATIVE_CUTOFF, seconds);
            derivative = derivativeAlpha * speed + (1.0D - derivativeAlpha) * derivative;
            double valueAlpha = alpha(MINIMUM_CUTOFF + SPEED_RESPONSE * Math.abs(derivative), seconds);
            filtered = valueAlpha * value + (1.0D - valueAlpha) * filtered;
            return filtered;
        }
    }
}
