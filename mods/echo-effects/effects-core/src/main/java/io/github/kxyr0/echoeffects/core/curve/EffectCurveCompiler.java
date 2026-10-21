package io.github.kxyr0.echoeffects.core.curve;

import io.github.kxyr0.echoeffects.api.curve.EffectCurveKey;
import io.github.kxyr0.echoeffects.api.curve.EffectFloatCurve;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnostic;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnosticCode;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectResult;
import java.util.ArrayList;
import java.util.List;

public final class EffectCurveCompiler {
    public EffectResult<EffectFloatCurve> compile(EffectFloatCurve curve, String source) {
        List<EffectDiagnostic> diagnostics = validate(curve, source);
        if (diagnostics.stream().anyMatch(EffectDiagnostic::isError)) {
            return EffectResult.invalid(diagnostics);
        }
        return EffectResult.valid(curve, diagnostics);
    }

    private List<EffectDiagnostic> validate(EffectFloatCurve curve, String source) {
        List<EffectDiagnostic> diagnostics = new ArrayList<>();
        long previous = -1L;
        for (EffectCurveKey key : curve.keys()) {
            if (key.tick() <= previous) {
                diagnostics.add(EffectDiagnostic.error(EffectDiagnosticCode.EFFECT_INVALID_CURVE,
                        "Curve keys must be strictly sorted by effect tick.", source));
            }
            previous = key.tick();
        }
        return diagnostics;
    }
}
