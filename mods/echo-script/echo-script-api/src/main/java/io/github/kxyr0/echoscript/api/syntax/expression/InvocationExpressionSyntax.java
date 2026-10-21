package io.github.kxyr0.echoscript.api.syntax.expression;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable invocation expression syntax.
 */
public record InvocationExpressionSyntax(
        ExpressionSyntax target,
        List<ArgumentSyntax> arguments,
        SourceSpan span
) implements ExpressionSyntax {
    public InvocationExpressionSyntax {
        target = Objects.requireNonNull(target, "target");
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        span = Objects.requireNonNull(span, "span");
    }
}
