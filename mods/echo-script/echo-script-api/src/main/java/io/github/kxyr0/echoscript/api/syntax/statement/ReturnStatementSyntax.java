package io.github.kxyr0.echoscript.api.syntax.statement;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable return statement syntax.
 */
public record ReturnStatementSyntax(
        Optional<ExpressionSyntax> value,
        SourceSpan span
) implements StatementSyntax {
    public ReturnStatementSyntax {
        value = Objects.requireNonNull(value, "value");
        span = Objects.requireNonNull(span, "span");
    }
}
