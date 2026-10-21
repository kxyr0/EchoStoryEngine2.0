package io.github.kxyr0.echoscript.api.syntax.statement;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable local `var` declaration syntax.
 */
public record LocalDeclarationStatementSyntax(
        String name,
        Optional<ExpressionSyntax> initializer,
        SourceSpan span
) implements StatementSyntax {
    public LocalDeclarationStatementSyntax {
        name = Objects.requireNonNull(name, "name");
        initializer = Objects.requireNonNull(initializer, "initializer");
        span = Objects.requireNonNull(span, "span");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Local variable name must not be blank.");
        }
    }
}
