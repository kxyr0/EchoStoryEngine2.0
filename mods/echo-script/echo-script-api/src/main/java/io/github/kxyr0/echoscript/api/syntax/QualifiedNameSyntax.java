package io.github.kxyr0.echoscript.api.syntax;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable dotted-name syntax.
 */
public record QualifiedNameSyntax(List<String> segments, SourceSpan span) implements SyntaxNode {
    public QualifiedNameSyntax {
        segments = List.copyOf(Objects.requireNonNull(segments, "segments"));
        span = Objects.requireNonNull(span, "span");
        if (segments.isEmpty() || segments.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Qualified names require non-blank segments.");
        }
    }

    public String text() {
        return String.join(".", segments);
    }
}
