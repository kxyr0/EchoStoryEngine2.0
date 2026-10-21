package io.github.kxyr0.echoeffects.echoscript.diagnostic;

import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnostic;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnosticCode;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnosticSeverity;
import io.github.kxyr0.echoscript.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoscript.api.diagnostic.LanguageDiagnostic;
import java.util.List;

public final class EchoScriptDiagnosticAdapter {
    public List<EffectDiagnostic> adapt(List<LanguageDiagnostic> diagnostics) {
        return diagnostics.stream().map(this::adapt).toList();
    }

    private EffectDiagnostic adapt(LanguageDiagnostic diagnostic) {
        EffectDiagnosticSeverity severity = diagnostic.severity() == DiagnosticSeverity.ERROR
                ? EffectDiagnosticSeverity.ERROR : EffectDiagnosticSeverity.WARNING;
        return new EffectDiagnostic(EffectDiagnosticCode.EFFECT_RESOURCE_PARSE_FAILED, severity,
                diagnostic.message(), diagnostic.sourceId().value());
    }
}
