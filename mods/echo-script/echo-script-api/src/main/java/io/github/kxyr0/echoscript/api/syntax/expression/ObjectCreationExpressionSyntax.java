package io.github.kxyr0.echoscript.api.syntax.expression;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.declaration.TypeSyntax;
import java.util.List;
import java.util.Objects;

/**
 * Immutable object-creation expression syntax.
 */
public record ObjectCreationExpressionSyntax(
        TypeSyntax type,
        List<ArgumentSyntax> arguments,
        SourceSpan span
) implements ExpressionSyntax {
    public ObjectCreationExpressionSyntax {
        type = Objects.requireNonNull(type, "type");
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        span = Objects.requireNonNull(span, "span");
    }
}
