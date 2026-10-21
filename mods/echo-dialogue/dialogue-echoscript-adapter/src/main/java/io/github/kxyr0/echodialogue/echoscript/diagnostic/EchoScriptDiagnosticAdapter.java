package io.github.kxyr0.echodialogue.echoscript.diagnostic;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echoscript.api.diagnostic.LanguageDiagnostic;
import java.util.List;
import java.util.Optional;

/**
 * Converts EchoScript syntax diagnostics into Echo Dialogue diagnostics.
 */
public final class EchoScriptDiagnosticAdapter {
    public List<DialogueDiagnostic> adapt(List<LanguageDiagnostic> diagnostics) {
        return diagnostics.stream().map(this::adaptOne).toList();
    }

    private DialogueDiagnostic adaptOne(LanguageDiagnostic diagnostic) {
        return new DialogueDiagnostic(
                "EDLG-SYNTAX-" + diagnostic.code(),
                severity(diagnostic),
                DiagnosticCategory.SYNTAX_ADAPTATION,
                diagnostic.message(),
                subject(diagnostic));
    }

    private Optional<DiagnosticSubject> subject(LanguageDiagnostic diagnostic) {
        return diagnostic.span()
                .flatMap(span -> DiagnosticSubject.source(
                        diagnostic.sourceId().value(),
                        span.start().line(),
                        span.start().column()))
                .or(() -> DiagnosticSubject.source(diagnostic.sourceId().value()));
    }

    private DiagnosticSeverity severity(LanguageDiagnostic diagnostic) {
        return switch (diagnostic.severity().name()) {
            case "INFO" -> DiagnosticSeverity.INFO;
            case "WARNING" -> DiagnosticSeverity.WARNING;
            default -> DiagnosticSeverity.ERROR;
        };
    }
}
