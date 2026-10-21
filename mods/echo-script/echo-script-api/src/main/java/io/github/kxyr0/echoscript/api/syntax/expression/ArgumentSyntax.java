package io.github.kxyr0.echoscript.api.syntax.expression;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.SyntaxNode;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable positional or named invocation argument syntax.
 */
public record ArgumentSyntax(
        Optional<String> name,
        ExpressionSyntax value,
        SourceSpan span
) implements SyntaxNode {
    public ArgumentSyntax {
        name = Objects.requireNonNull(name, "name");
        value = Objects.requireNonNull(value, "value");
        span = Objects.requireNonNull(span, "span");
        name.ifPresent(argumentName -> {
            if (argumentName.isBlank()) {
                throw new IllegalArgumentException("Argument name must not be blank.");
            }
        });
    }
}
