package io.github.kxyr0.echoscript.api.syntax.expression;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.List;
import java.util.Objects;

/**
 * Immutable array literal expression syntax.
 */
public record ArrayLiteralExpressionSyntax(
        List<ExpressionSyntax> elements,
        SourceSpan span
) implements ExpressionSyntax {
    public ArrayLiteralExpressionSyntax {
        elements = List.copyOf(Objects.requireNonNull(elements, "elements"));
        span = Objects.requireNonNull(span, "span");
    }
}
