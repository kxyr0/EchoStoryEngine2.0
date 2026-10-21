package io.github.kxyr0.echomagic.api.diagnostic;

import java.time.Instant;
import java.util.Objects;

public record MagicDiagnostic(MagicDiagnosticCode code, MagicDiagnosticSeverity severity, String message,
                              String source, Instant occurredAt) {
    public MagicDiagnostic {
        code = Objects.requireNonNull(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = Objects.requireNonNull(message, "message");
        source = Objects.requireNonNull(source, "source");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public static MagicDiagnostic error(MagicDiagnosticCode code, String message, String source) {
        return new MagicDiagnostic(code, MagicDiagnosticSeverity.ERROR, message, source, Instant.now());
    }

    public boolean isError() { return severity == MagicDiagnosticSeverity.ERROR; }
}

