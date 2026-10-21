package io.github.kxyr0.echoscript.api.syntax.statement;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.Objects;

/**
 * Immutable intent section consisting of an invocation header and a nested statement block.
 */
public record SectionStatementSyntax(
        InvocationExpressionSyntax invocation,
        BlockStatementSyntax body,
        SourceSpan span
) implements StatementSyntax {
    public SectionStatementSyntax {
        invocation = Objects.requireNonNull(invocation, "invocation");
        body = Objects.requireNonNull(body, "body");
        span = Objects.requireNonNull(span, "span");
    }
}
