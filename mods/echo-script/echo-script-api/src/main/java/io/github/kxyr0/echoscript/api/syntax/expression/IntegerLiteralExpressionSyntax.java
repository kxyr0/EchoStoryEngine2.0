package io.github.kxyr0.echoscript.api.syntax.expression;

import io.github.kxyr0.echoscript.api.source.SourceSpan;

/**
 * Immutable integer literal expression syntax.
 */
public record IntegerLiteralExpressionSyntax(int value, SourceSpan span) implements ExpressionSyntax {
    public IntegerLiteralExpressionSyntax {
        if (span == null) {
            throw new NullPointerException("span");
        }
    }
}
