package io.github.kxyr0.echoscript.api.syntax.declaration;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.SyntaxModifier;
import io.github.kxyr0.echoscript.api.syntax.statement.BlockStatementSyntax;
import java.util.List;
import java.util.Objects;

/**
 * Immutable method declaration syntax.
 */
public record MethodSyntax(
        List<SyntaxModifier> modifiers,
        TypeSyntax returnType,
        String name,
        List<ParameterSyntax> parameters,
        BlockStatementSyntax body,
        SourceSpan span
) implements MemberSyntax {
    public MethodSyntax {
        modifiers = List.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
        returnType = Objects.requireNonNull(returnType, "returnType");
        name = Objects.requireNonNull(name, "name");
        parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
        body = Objects.requireNonNull(body, "body");
        span = Objects.requireNonNull(span, "span");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Method name must not be blank.");
        }
    }
}
