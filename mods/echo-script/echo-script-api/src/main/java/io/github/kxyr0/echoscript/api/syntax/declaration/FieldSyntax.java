package io.github.kxyr0.echoscript.api.syntax.declaration;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.SyntaxModifier;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable field declaration syntax.
 */
public record FieldSyntax(
        List<SyntaxModifier> modifiers,
        TypeSyntax type,
        String name,
        Optional<ExpressionSyntax> initializer,
        SourceSpan span
) implements MemberSyntax {
    public FieldSyntax {
        modifiers = List.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
        type = Objects.requireNonNull(type, "type");
        name = Objects.requireNonNull(name, "name");
        initializer = Objects.requireNonNull(initializer, "initializer");
        span = Objects.requireNonNull(span, "span");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Field name must not be blank.");
        }
    }
}
