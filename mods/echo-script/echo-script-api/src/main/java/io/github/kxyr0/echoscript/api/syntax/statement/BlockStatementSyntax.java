package io.github.kxyr0.echoscript.api.syntax.statement;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable block statement syntax.
 */
public record BlockStatementSyntax(List<StatementSyntax> statements, SourceSpan span) implements StatementSyntax {
    public BlockStatementSyntax {
        statements = List.copyOf(Objects.requireNonNull(statements, "statements"));
        span = Objects.requireNonNull(span, "span");
    }
}
