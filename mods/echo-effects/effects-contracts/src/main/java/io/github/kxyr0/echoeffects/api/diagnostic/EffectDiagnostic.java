package io.github.kxyr0.echoeffects.api.diagnostic;

import java.util.Objects;

public record EffectDiagnostic(
        EffectDiagnosticCode code,
        EffectDiagnosticSeverity severity,
        String message,
        String source
) {
    public EffectDiagnostic {
        code = Objects.requireNonNull(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = Objects.requireNonNull(message, "message");
        source = Objects.requireNonNull(source, "source");
    }

    public static EffectDiagnostic error(EffectDiagnosticCode code, String message, String source) {
        return new EffectDiagnostic(code, EffectDiagnosticSeverity.ERROR, message, source);
    }

    public boolean isError() {
        return severity == EffectDiagnosticSeverity.ERROR;
    }
}
