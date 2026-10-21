package io.github.kxyr0.echoscript.api.syntax.statement;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable syntax-only if/else statement.
 */
public record IfStatementSyntax(
        ExpressionSyntax condition,
        StatementSyntax thenStatement,
        Optional<StatementSyntax> elseStatement,
        SourceSpan span
) implements StatementSyntax {
    public IfStatementSyntax {
        condition = Objects.requireNonNull(condition, "condition");
        thenStatement = Objects.requireNonNull(thenStatement, "thenStatement");
        elseStatement = Objects.requireNonNull(elseStatement, "elseStatement");
        span = Objects.requireNonNull(span, "span");
    }
}
