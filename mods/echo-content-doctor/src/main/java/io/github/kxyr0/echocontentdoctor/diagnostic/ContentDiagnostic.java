package io.github.kxyr0.echocontentdoctor.diagnostic;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import java.util.List;
import java.util.Objects;

public record ContentDiagnostic(
        ContentSeverity severity,
        String code,
        ContentResourceId resource,
        SourceSpan span,
        String message,
        List<ContentResourceId> suggestions
) implements Comparable<ContentDiagnostic> {
    public ContentDiagnostic {
        severity = Objects.requireNonNull(severity, "severity");
        code = Objects.requireNonNull(code, "code");
        resource = Objects.requireNonNull(resource, "resource");
        span = Objects.requireNonNull(span, "span");
        message = Objects.requireNonNull(message, "message");
        suggestions = List.copyOf(suggestions);
    }

    @Override
    public int compareTo(ContentDiagnostic other) {
        int bySource = span.sourceId().value().compareTo(other.span.sourceId().value());
        if (bySource != 0) return bySource;
        int byOffset = Integer.compare(span.startOffset(), other.span.startOffset());
        return byOffset == 0 ? code.compareTo(other.code) : byOffset;
    }
}
