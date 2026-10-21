package io.github.kxyr0.echoscript.language.diagnostic;

import io.github.kxyr0.echoscript.api.ProcessingLimits;
import io.github.kxyr0.echoscript.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoscript.api.diagnostic.LanguageDiagnostic;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.language.source.SourceText;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DiagnosticBag {
    private static final int CONTEXT_LIMIT = 120;

    private final SourceText source;
    private final ProcessingLimits limits;
    private final List<LanguageDiagnostic> diagnostics = new ArrayList<>();
    private boolean halted;

    public DiagnosticBag(SourceText source, ProcessingLimits limits) {
        this.source = source;
        this.limits = limits;
    }

    public void report(String code, SourceSpan span, String message) {
        report(code, span, message, Optional.empty());
    }

    public void report(String code, SourceSpan span, String message, Optional<String> hint) {
        if (halted) {
            return;
        }
        if (diagnostics.size() >= limits.maxDiagnostics() - 1) {
            addLimitDiagnostic(span);
            halted = true;
            return;
        }
        diagnostics.add(new LanguageDiagnostic(
                code,
                DiagnosticSeverity.ERROR,
                source.sourceId(),
                Optional.ofNullable(span),
                message,
                span == null ? "" : source.context(span, CONTEXT_LIMIT),
                hint
        ));
    }

    public boolean halted() {
        return halted;
    }

    public boolean hasErrors() {
        return !diagnostics.isEmpty();
    }

    public List<LanguageDiagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }

    private void addLimitDiagnostic(SourceSpan span) {
        diagnostics.add(new LanguageDiagnostic(
                DiagnosticCodes.DIAGNOSTIC_LIMIT_EXCEEDED,
                DiagnosticSeverity.ERROR,
                source.sourceId(),
                Optional.ofNullable(span),
                "Diagnostic limit exceeded.",
                span == null ? "" : source.context(span, CONTEXT_LIMIT),
                Optional.of("Increase ProcessingLimits.maxDiagnostics only for trusted sources.")
        ));
    }
}
