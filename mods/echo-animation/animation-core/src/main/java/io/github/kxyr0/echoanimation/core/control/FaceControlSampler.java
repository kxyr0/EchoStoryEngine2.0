package io.github.kxyr0.echoanimation.core.control;

import io.github.kxyr0.echoanimation.api.math.Vec2;

/**
 * Shared normalized face-control math.
 */
public final class FaceControlSampler {
    public double eyeClosureTravel(double normalized, double configuredTravel) {
        double clamped = clamp01(normalized);
        return clamped * configuredTravel;
    }

    public Vec2 clampGaze(Vec2 requested, Vec2 min, Vec2 max) {
        double x = Math.max(min.x(), Math.min(max.x(), requested.x()));
        double y = Math.max(min.y(), Math.min(max.y(), requested.y()));
        return new Vec2(x, y);
    }

    private double clamp01(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Control value должен быть finite.");
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}

