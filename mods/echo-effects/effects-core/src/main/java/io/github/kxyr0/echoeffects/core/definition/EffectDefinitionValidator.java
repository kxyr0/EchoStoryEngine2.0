package io.github.kxyr0.echoeffects.core.definition;

import io.github.kxyr0.echoeffects.api.definition.EffectDefinition;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnostic;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnosticCode;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectResult;
import io.github.kxyr0.echoeffects.core.curve.EffectCurveCompiler;
import java.util.ArrayList;
import java.util.List;

public final class EffectDefinitionValidator {
    private final EffectCurveCompiler curveCompiler = new EffectCurveCompiler();

    public EffectResult<EffectDefinition> validate(EffectDefinition definition, String source) {
        List<EffectDiagnostic> diagnostics = new ArrayList<>();
        if (!definition.duration().infiniteDuration() && definition.duration().ticks() <= 0L) {
            diagnostics.add(EffectDiagnostic.error(EffectDiagnosticCode.EFFECT_INVALID_DURATION,
                    "Finite duration must be positive.", source));
        }
        definition.tracks().forEach(track ->
                diagnostics.addAll(curveCompiler.compile(track.curve(), source).diagnostics()));
        if (diagnostics.stream().anyMatch(EffectDiagnostic::isError)) {
            return EffectResult.invalid(diagnostics);
        }
        return EffectResult.valid(definition, diagnostics);
    }
}
