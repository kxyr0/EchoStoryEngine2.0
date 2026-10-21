package io.github.kxyr0.echonpc.echoscript.binding;

import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnosticSeverity;
import io.github.kxyr0.echoscript.api.source.SourceSpan;

public final class SemanticDiagnosticFactory {
    private SemanticDiagnosticFactory() {
    }

    public static NpcDiagnostic error(String code, String message, String context, SourceSpan span) {
        return new NpcDiagnostic(code, NpcDiagnosticSeverity.ERROR, message, context,
                java.util.Optional.of(span.start().line()), java.util.Optional.of(span.start().column()));
    }
}
