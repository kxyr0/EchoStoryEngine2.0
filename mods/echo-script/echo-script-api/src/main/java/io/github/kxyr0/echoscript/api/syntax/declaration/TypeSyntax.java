package io.github.kxyr0.echoscript.api.syntax.declaration;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.QualifiedNameSyntax;
import io.github.kxyr0.echoscript.api.syntax.SyntaxNode;
import java.util.Objects;

/**
 * Immutable type-name syntax with optional array rank.
 */
public record TypeSyntax(QualifiedNameSyntax name, int arrayRank, SourceSpan span) implements SyntaxNode {
    public TypeSyntax {
        name = Objects.requireNonNull(name, "name");
        span = Objects.requireNonNull(span, "span");
        if (arrayRank < 0) {
            throw new IllegalArgumentException("Array rank must not be negative.");
        }
    }

    public boolean isVoid() {
        return arrayRank == 0 && "void".equals(name.text());
    }
}
