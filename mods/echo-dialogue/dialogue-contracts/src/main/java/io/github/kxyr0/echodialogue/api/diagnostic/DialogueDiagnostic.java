package io.github.kxyr0.echodialogue.api.diagnostic;

import java.util.Objects;
import java.util.Optional;

/**
 * Structured diagnostic returned by non-logging modules.
 */
public record DialogueDiagnostic(
        String code,
        DiagnosticSeverity severity,
        DiagnosticCategory category,
        String message,
        Optional<DiagnosticSubject> subject
) {
    public DialogueDiagnostic {
        code = Objects.requireNonNull(code, "code").trim();
        severity = Objects.requireNonNull(severity, "severity");
        category = Objects.requireNonNull(category, "category");
        message = Objects.requireNonNull(message, "message").trim();
        subject = Objects.requireNonNull(subject, "subject");
        if (code.isEmpty() || message.isEmpty()) {
            throw new IllegalArgumentException("Diagnostic code and message must be non-empty.");
        }
    }

    public static DialogueDiagnostic error(String code, DiagnosticCategory category, String message) {
        return new DialogueDiagnostic(code, DiagnosticSeverity.ERROR, category, message, DiagnosticSubject.none());
    }

    public static DialogueDiagnostic info(String code, DiagnosticCategory category, String message) {
        return new DialogueDiagnostic(code, DiagnosticSeverity.INFO, category, message, DiagnosticSubject.none());
    }

    public static DialogueDiagnostic warning(String code, DiagnosticCategory category, String message) {
        return new DialogueDiagnostic(code, DiagnosticSeverity.WARNING, category, message, DiagnosticSubject.none());
    }

    public boolean isError() {
        return severity == DiagnosticSeverity.ERROR;
    }
}
