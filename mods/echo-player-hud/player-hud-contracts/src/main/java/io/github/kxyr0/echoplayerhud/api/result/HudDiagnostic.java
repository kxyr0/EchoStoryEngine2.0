package io.github.kxyr0.echoplayerhud.api.result;

import java.util.Objects;

/**
 * Human-readable stable diagnostic emitted by HUD boundaries.
 */
public record HudDiagnostic(
        String code,
        HudDiagnosticSeverity severity,
        String message,
        String source
) {
    public HudDiagnostic {
        code = requireText(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = requireText(message, "message");
        source = Objects.requireNonNull(source, "source").strip();
    }

    public static HudDiagnostic error(String code, String message, String source) {
        return new HudDiagnostic(code, HudDiagnosticSeverity.ERROR, message, source);
    }

    public static HudDiagnostic warning(String code, String message, String source) {
        return new HudDiagnostic(code, HudDiagnosticSeverity.WARNING, message, source);
    }

    public boolean error() {
        return severity == HudDiagnosticSeverity.ERROR;
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank.");
        }
        return normalized;
    }
}
