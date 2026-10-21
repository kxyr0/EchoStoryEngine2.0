package io.github.kxyr0.echoanimation.echoscript.diagnostic;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoscript.api.diagnostic.LanguageDiagnostic;
import java.util.List;

/**
 * Converts syntax-only EchoScript diagnostics to Echo Animation diagnostics.
 */
public final class EchoScriptDiagnosticAdapter {
    public List<AnimationDiagnostic> adapt(List<LanguageDiagnostic> diagnostics) {
        return diagnostics.stream().map(this::adaptOne).toList();
    }

    private AnimationDiagnostic adaptOne(LanguageDiagnostic diagnostic) {
        DiagnosticSeverity severity = diagnostic.severity().name().equals("ERROR")
                ? DiagnosticSeverity.ERROR
                : DiagnosticSeverity.WARNING;
        String message = AnimationDiagnosticMessages.translate(diagnostic.message())
                .replace("Ошибка анимации: ", "");
        AnimationDiagnostic adapted = new AnimationDiagnostic(
                diagnostic.code(),
                severity,
                "Ошибка EchoScript: " + message);
        return diagnostic.span()
                .map(span -> AnimationDiagnosticMessages.source(adapted, span))
                .orElseGet(() -> AnimationDiagnosticMessages.localized(adapted));
    }
}
