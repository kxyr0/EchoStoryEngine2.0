package io.github.kxyr0.echoscript.api.syntax.declaration;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.BlockStatementSyntax;
import java.util.Objects;

/** Immutable class-level intent section with an invocation header and body. */
public record ClassSectionSyntax(
        InvocationExpressionSyntax invocation,
        BlockStatementSyntax body,
        SourceSpan span
) implements MemberSyntax {
    public ClassSectionSyntax {
        invocation = Objects.requireNonNull(invocation, "invocation");
        body = Objects.requireNonNull(body, "body");
        span = Objects.requireNonNull(span, "span");
    }
}
