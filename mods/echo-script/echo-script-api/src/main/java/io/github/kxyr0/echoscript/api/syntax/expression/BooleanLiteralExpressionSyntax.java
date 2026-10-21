package io.github.kxyr0.echoscript.api.syntax.expression;

import io.github.kxyr0.echoscript.api.source.SourceSpan;

/**
 * Immutable boolean literal expression syntax.
 */
public record BooleanLiteralExpressionSyntax(boolean value, SourceSpan span) implements ExpressionSyntax {
    public BooleanLiteralExpressionSyntax {
        if (span == null) {
            throw new NullPointerException("span");
        }
    }
}
