package io.github.kxyr0.echosavemigration.compile;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;

public record MigrationDiagnostic(String code, String message, SourceSpan span) {
    public MigrationDiagnostic {
        code = Objects.requireNonNull(code, "code");
        message = Objects.requireNonNull(message, "message");
        span = Objects.requireNonNull(span, "span");
    }
}
