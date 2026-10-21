package io.github.kxyr0.echoscript.api.syntax.declaration;

import io.github.kxyr0.echoscript.api.syntax.SyntaxNode;

/**
 * Sealed immutable class-member syntax contract.
 */
public sealed interface MemberSyntax extends SyntaxNode permits ClassCallSyntax, ClassSectionSyntax,
        DialogueNodeDeclarationSyntax, FieldSyntax, MethodSyntax {
}
