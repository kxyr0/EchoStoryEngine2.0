package io.github.kxyr0.echoscript.api.syntax;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.TypeSyntax;
import java.util.List;
import java.util.Objects;

/**
 * Immutable class declaration syntax.
 *
 * <p>Base type names are syntax only. EchoScript does not resolve or validate consumer-domain types.</p>
 */
public record ClassSyntax(
        List<SyntaxModifier> modifiers,
        String name,
        TypeSyntax baseType,
        List<MemberSyntax> members,
        SourceSpan span
) implements SyntaxNode {
    public ClassSyntax {
        modifiers = List.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
        name = Objects.requireNonNull(name, "name");
        baseType = Objects.requireNonNull(baseType, "baseType");
        members = List.copyOf(Objects.requireNonNull(members, "members"));
        span = Objects.requireNonNull(span, "span");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Class name must not be blank.");
        }
    }
}
