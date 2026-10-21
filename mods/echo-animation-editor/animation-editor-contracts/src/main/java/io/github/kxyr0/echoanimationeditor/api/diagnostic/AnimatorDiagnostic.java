package io.github.kxyr0.echoanimationeditor.api.diagnostic;

import java.util.Objects;
import java.util.Optional;

public record AnimatorDiagnostic(
        String code,
        AnimatorDiagnosticSeverity severity,
        String message,
        Optional<String> source
) {
    public AnimatorDiagnostic {
        code = requireText(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = requireText(message, "message");
        source = Objects.requireNonNull(source, "source");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " is blank");
        }
        return value;
    }
}
