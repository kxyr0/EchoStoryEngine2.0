package io.github.kxyr0.echoscript.api.syntax.statement;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import java.util.Objects;

/**
 * Immutable expression statement syntax.
 */
public record ExpressionStatementSyntax(ExpressionSyntax expression, SourceSpan span) implements StatementSyntax {
    public ExpressionStatementSyntax {
        expression = Objects.requireNonNull(expression, "expression");
        span = Objects.requireNonNull(span, "span");
    }
}
