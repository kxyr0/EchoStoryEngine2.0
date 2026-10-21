package io.github.kxyr0.echobackgroundnpc.api.diagnostic;

import io.github.kxyr0.echobackgroundnpc.api.source.BackgroundSourceRef;
import java.util.Objects;
import java.util.Optional;

public record BackgroundDiagnostic(
        BackgroundDiagnosticCode code,
        Severity severity,
        String message,
        Optional<BackgroundSourceRef> source
) {
    public BackgroundDiagnostic {
        code = Objects.requireNonNull(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = Objects.requireNonNull(message, "message").trim();
        source = Objects.requireNonNull(source, "source");
    }
    public boolean error() { return severity == Severity.ERROR; }
    public enum Severity { INFO, WARNING, ERROR }
}
