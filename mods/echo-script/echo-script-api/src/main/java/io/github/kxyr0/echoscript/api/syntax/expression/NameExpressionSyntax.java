package io.github.kxyr0.echoscript.api.syntax.expression;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;

/**
 * Immutable identifier expression syntax.
 */
public record NameExpressionSyntax(String name, SourceSpan span) implements ExpressionSyntax {
    public NameExpressionSyntax {
        name = Objects.requireNonNull(name, "name");
        span = Objects.requireNonNull(span, "span");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name expression must not be blank.");
        }
    }
}
