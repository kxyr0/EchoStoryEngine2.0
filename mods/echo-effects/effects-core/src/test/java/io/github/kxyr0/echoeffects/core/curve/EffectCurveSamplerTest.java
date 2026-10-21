package io.github.kxyr0.echoeffects.core.curve;

import io.github.kxyr0.echoeffects.api.curve.EffectCurveKey;
import io.github.kxyr0.echoeffects.api.curve.EffectFloatCurve;
import io.github.kxyr0.echoeffects.api.curve.EffectInterpolation;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EffectCurveSamplerTest {
    private final EffectCurveSampler sampler = new EffectCurveSampler();

    @Test
    void linearSamplesMidpoint() {
        EffectFloatCurve curve = new EffectFloatCurve(List.of(
                new EffectCurveKey(0L, 1.0D, EffectInterpolation.LINEAR),
                new EffectCurveKey(20L, 0.5D, EffectInterpolation.LINEAR)));

        Assertions.assertEquals(0.75D, sampler.sample(curve, 10.0D), 0.0001D);
    }

    @Test
    void stepHoldsLeftValueUntilNextKey() {
        EffectFloatCurve curve = new EffectFloatCurve(List.of(
                new EffectCurveKey(0L, 1.0D, EffectInterpolation.LINEAR),
                new EffectCurveKey(20L, 0.5D, EffectInterpolation.STEP)));

        Assertions.assertEquals(1.0D, sampler.sample(curve, 10.0D), 0.0001D);
    }
}
