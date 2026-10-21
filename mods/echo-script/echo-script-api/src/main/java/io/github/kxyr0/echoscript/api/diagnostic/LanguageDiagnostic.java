package io.github.kxyr0.echoscript.api.diagnostic;

import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable structured diagnostic produced by the EchoScript language frontend.
 *
 * <p>Diagnostics contain stable codes, one logical source identity, optional spans, bounded context, and optional
 * hints. Expected syntax failures are represented here without stack traces or logging side effects.</p>
 */
public record LanguageDiagnostic(
        String code,
        DiagnosticSeverity severity,
        SourceId sourceId,
        Optional<SourceSpan> span,
        String message,
        String context,
        Optional<String> hint
) {
    public LanguageDiagnostic {
        code = requireText(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        span = Objects.requireNonNull(span, "span");
        message = requireText(message, "message");
        context = Objects.requireNonNull(context, "context");
        hint = Objects.requireNonNull(hint, "hint");
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name);
        if (checked.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
