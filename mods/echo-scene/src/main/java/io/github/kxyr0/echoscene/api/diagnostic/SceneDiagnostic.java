package io.github.kxyr0.echoscene.api.diagnostic;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;

public record SceneDiagnostic(Severity severity, String code, String message, SourceSpan span) {
    public SceneDiagnostic {
        severity = Objects.requireNonNull(severity, "severity");
        code = Objects.requireNonNull(code, "code");
        message = Objects.requireNonNull(message, "message");
        span = Objects.requireNonNull(span, "span");
    }

    public enum Severity {
        ERROR,
        WARNING
    }
}
