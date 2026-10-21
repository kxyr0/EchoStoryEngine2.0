package io.github.kxyr0.echoscript.api.syntax.expression;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;

/**
 * Immutable parenthesized expression syntax.
 */
public record ParenthesizedExpressionSyntax(ExpressionSyntax expression, SourceSpan span) implements ExpressionSyntax {
    public ParenthesizedExpressionSyntax {
        expression = Objects.requireNonNull(expression, "expression");
        span = Objects.requireNonNull(span, "span");
    }
}
