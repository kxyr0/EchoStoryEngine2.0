package io.github.kxyr0.echoscript.api.syntax.declaration;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.SyntaxNode;
import java.util.Objects;

/**
 * Immutable method-parameter syntax.
 */
public record ParameterSyntax(TypeSyntax type, String name, SourceSpan span) implements SyntaxNode {
    public ParameterSyntax {
        type = Objects.requireNonNull(type, "type");
        name = Objects.requireNonNull(name, "name");
        span = Objects.requireNonNull(span, "span");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Parameter name must not be blank.");
        }
    }
}
