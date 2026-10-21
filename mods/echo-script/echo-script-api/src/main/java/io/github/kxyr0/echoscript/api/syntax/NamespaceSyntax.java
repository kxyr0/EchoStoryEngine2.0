package io.github.kxyr0.echoscript.api.syntax;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Objects;

/**
 * Immutable namespace declaration syntax.
 */
public record NamespaceSyntax(QualifiedNameSyntax name, SourceSpan span) implements SyntaxNode {
    public NamespaceSyntax {
        name = Objects.requireNonNull(name, "name");
        span = Objects.requireNonNull(span, "span");
    }
}
