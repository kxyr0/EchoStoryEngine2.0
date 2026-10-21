package io.github.kxyr0.echoanimation.api.diagnostic;

import java.util.Objects;

/**
 * Immutable Russian diagnostic with stable English code.
 */
public record AnimationDiagnostic(String code, DiagnosticSeverity severity, String message) {
    public AnimationDiagnostic {
        code = Objects.requireNonNull(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = Objects.requireNonNull(message, "message");
        if (code.isBlank() || message.isBlank()) {
            throw new IllegalArgumentException("Diagnostic code/message не должны быть пустыми.");
        }
    }

    public boolean error() {
        return severity == DiagnosticSeverity.ERROR;
    }
}

