package io.github.kxyr0.echowardrobe.api.diagnostic;

import java.util.Objects;

/**
 * Source-linked structured diagnostic.
 */
public record WardrobeDiagnostic(
        WardrobeDiagnosticCode code,
        Severity severity,
        String message,
        String source
) {
    public WardrobeDiagnostic {
        code = Objects.requireNonNull(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = Objects.requireNonNull(message, "message");
        source = Objects.requireNonNull(source, "source");
    }

    public static WardrobeDiagnostic error(WardrobeDiagnosticCode code, String message, String source) {
        return new WardrobeDiagnostic(code, Severity.ERROR, message, source);
    }

    public static WardrobeDiagnostic warning(WardrobeDiagnosticCode code, String message, String source) {
        return new WardrobeDiagnostic(code, Severity.WARN, message, source);
    }

    public boolean error() {
        return severity == Severity.ERROR;
    }

    public enum Severity {
        INFO,
        WARN,
        ERROR
    }
}
