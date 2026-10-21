package io.github.kxyr0.echoplayer.api.result;

import java.util.Objects;
import java.util.Optional;

/**
 * Bounded profile diagnostic emitted by parsing, binding, validation, or recovery.
 */
public record PlayerProfileDiagnostic(
        String code,
        PlayerDiagnosticSeverity severity,
        String message,
        Optional<String> source
) {
    public PlayerProfileDiagnostic {
        code = requireText(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = requireText(message, "message");
        source = Objects.requireNonNull(source, "source");
    }

    public static PlayerProfileDiagnostic error(String code, String message, String source) {
        return new PlayerProfileDiagnostic(code, PlayerDiagnosticSeverity.ERROR, message, Optional.of(source));
    }

    public static PlayerProfileDiagnostic warning(String code, String message, String source) {
        return new PlayerProfileDiagnostic(code, PlayerDiagnosticSeverity.WARNING, message, Optional.of(source));
    }

    public boolean error() {
        return severity == PlayerDiagnosticSeverity.ERROR;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
        return value;
    }
}
