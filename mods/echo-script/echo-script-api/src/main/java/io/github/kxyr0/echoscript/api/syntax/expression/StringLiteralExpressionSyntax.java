package io.github.kxyr0.echoscript.api.syntax.expression;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;

/**
 * Immutable string literal expression syntax.
 */
public record StringLiteralExpressionSyntax(String value, SourceSpan span) implements ExpressionSyntax {
    public StringLiteralExpressionSyntax {
        value = Objects.requireNonNull(value, "value");
        span = Objects.requireNonNull(span, "span");
    }
}
