package io.github.kxyr0.echoscript.api.syntax.declaration;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.SyntaxModifier;
import io.github.kxyr0.echoscript.api.syntax.statement.BlockStatementSyntax;
import java.util.List;
import java.util.Objects;

/**
 * Immutable high-level dialogue node declaration recognized in a DialogueResource.
 */
public record DialogueNodeDeclarationSyntax(
        List<SyntaxModifier> modifiers,
        boolean entry,
        String name,
        BlockStatementSyntax body,
        SourceSpan span
) implements MemberSyntax {
    public DialogueNodeDeclarationSyntax {
        modifiers = List.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
        name = Objects.requireNonNull(name, "name");
        body = Objects.requireNonNull(body, "body");
        span = Objects.requireNonNull(span, "span");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Dialogue node name must not be blank.");
        }
    }
}
