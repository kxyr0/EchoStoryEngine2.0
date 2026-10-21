package io.github.kxyr0.echoscript.api.syntax.expression;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable decimal number literal expression syntax.
 */
public record DecimalLiteralExpressionSyntax(BigDecimal value, SourceSpan span) implements ExpressionSyntax {
    public DecimalLiteralExpressionSyntax {
        value = Objects.requireNonNull(value, "value");
        span = Objects.requireNonNull(span, "span");
    }
}
