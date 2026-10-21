package io.github.kxyr0.echoeffects.core.curve;

import io.github.kxyr0.echoeffects.api.curve.EffectCurveKey;
import io.github.kxyr0.echoeffects.api.curve.EffectFloatCurve;
import io.github.kxyr0.echoeffects.api.curve.EffectInterpolation;
import java.util.List;

public final class EffectCurveSampler {
    public double sample(EffectFloatCurve curve, double tick) {
        List<EffectCurveKey> keys = curve.keys();
        if (tick <= keys.get(0).tick()) {
            return keys.get(0).value();
        }
        int lastIndex = keys.size() - 1;
        if (tick >= keys.get(lastIndex).tick()) {
            return keys.get(lastIndex).value();
        }
        for (int index = 1; index < keys.size(); index++) {
            EffectCurveKey right = keys.get(index);
            if (tick <= right.tick()) {
                return sampleSegment(keys.get(index - 1), right, tick);
            }
        }
        return keys.get(lastIndex).value();
    }

    private double sampleSegment(EffectCurveKey left, EffectCurveKey right, double tick) {
        if (right.interpolation() == EffectInterpolation.STEP) {
            return left.value();
        }
        double width = right.tick() - left.tick();
        double alpha = (tick - left.tick()) / width;
        if (right.interpolation() == EffectInterpolation.SMOOTH || right.interpolation() == EffectInterpolation.CUBIC) {
            alpha = alpha * alpha * (3.0D - 2.0D * alpha);
        }
        return left.value() + (right.value() - left.value()) * alpha;
    }
}
