package io.github.kxyr0.echonpc.echoscript.diagnostic;

import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnosticSeverity;
import io.github.kxyr0.echoscript.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoscript.api.diagnostic.LanguageDiagnostic;
import java.util.List;

/**
 * Adapts syntax-only EchoScript diagnostics into Echo NPC diagnostics without changing source text.
 */
public final class EchoScriptDiagnosticAdapter {
    public List<NpcDiagnostic> adapt(List<LanguageDiagnostic> diagnostics) {
        return diagnostics.stream().map(this::adaptOne).toList();
    }

    private NpcDiagnostic adaptOne(LanguageDiagnostic diagnostic) {
        NpcDiagnosticSeverity severity = diagnostic.severity() == DiagnosticSeverity.ERROR
                ? NpcDiagnosticSeverity.ERROR
                : NpcDiagnosticSeverity.WARNING;
        if (diagnostic.span().isPresent()) {
            var start = diagnostic.span().orElseThrow().start();
            return new NpcDiagnostic("ENPC-ECHOSCRIPT-" + diagnostic.code(), severity,
                    diagnostic.message(), diagnostic.context(), java.util.Optional.of(start.line()),
                    java.util.Optional.of(start.column()));
        }
        return new NpcDiagnostic("ENPC-ECHOSCRIPT-" + diagnostic.code(), severity, diagnostic.message(),
                diagnostic.context(), java.util.Optional.empty(), java.util.Optional.empty());
    }
}
