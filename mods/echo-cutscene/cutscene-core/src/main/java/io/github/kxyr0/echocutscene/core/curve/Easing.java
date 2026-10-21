package io.github.kxyr0.echocutscene.core.curve;

import io.github.kxyr0.echocutscene.api.camera.curve.CameraInterpolation;

final class Easing {
    private Easing() {
    }

    static double apply(CameraInterpolation interpolation, double t) {
        double clamped = Math.max(0.0D, Math.min(1.0D, t));
        if (interpolation == CameraInterpolation.SMOOTH) {
            return clamped * clamped * (3.0D - 2.0D * clamped);
        }
        return clamped;
    }
}
