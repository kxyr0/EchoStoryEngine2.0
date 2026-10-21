package io.github.kxyr0.echoplayervitals.api.result;

import java.util.Objects;

/**
 * Human-readable stable diagnostic emitted by Vitals boundaries.
 */
public record PlayerVitalsDiagnostic(
        String code,
        PlayerVitalsDiagnosticSeverity severity,
        String message,
        String source
) {
    public PlayerVitalsDiagnostic {
        code = requireText(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = requireText(message, "message");
        source = Objects.requireNonNull(source, "source").strip();
    }

    public static PlayerVitalsDiagnostic error(String code, String message, String source) {
        return new PlayerVitalsDiagnostic(code, PlayerVitalsDiagnosticSeverity.ERROR, message, source);
    }

    public static PlayerVitalsDiagnostic warning(String code, String message, String source) {
        return new PlayerVitalsDiagnostic(code, PlayerVitalsDiagnosticSeverity.WARNING, message, source);
    }

    public static PlayerVitalsDiagnostic info(String code, String message, String source) {
        return new PlayerVitalsDiagnostic(code, PlayerVitalsDiagnosticSeverity.INFO, message, source);
    }

    public boolean error() {
        return severity == PlayerVitalsDiagnosticSeverity.ERROR;
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank.");
        }
        return normalized;
    }
}
