package io.github.kxyr0.echoanimation.echoscript.diagnostic;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoscript.api.source.SourceSpan;

/**
 * Factory for Echo Animation semantic diagnostics.
 */
public final class AnimationSemanticDiagnostics {
    private AnimationSemanticDiagnostics() {
    }

    public static AnimationDiagnostic error(String code, String message) {
        return new AnimationDiagnostic(code, DiagnosticSeverity.ERROR,
                AnimationDiagnosticMessages.translate(message));
    }

    public static AnimationDiagnostic error(String code, String message, SourceSpan span) {
        AnimationDiagnostic diagnostic = new AnimationDiagnostic(code, DiagnosticSeverity.ERROR,
                AnimationDiagnosticMessages.translate(message));
        return AnimationDiagnosticMessages.source(diagnostic, span);
    }

    public static AnimationDiagnostic warning(String code, String message) {
        return new AnimationDiagnostic(code, DiagnosticSeverity.WARNING,
                AnimationDiagnosticMessages.translate(message));
    }
}
