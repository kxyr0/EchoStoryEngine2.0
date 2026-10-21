package io.github.kxyr0.echoscript.api.syntax.declaration;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.Objects;

/**
 * Immutable sparse class-level metadata call syntax.
 */
public record ClassCallSyntax(
        InvocationExpressionSyntax invocation,
        SourceSpan span
) implements MemberSyntax {
    public ClassCallSyntax {
        invocation = Objects.requireNonNull(invocation, "invocation");
        span = Objects.requireNonNull(span, "span");
    }
}
