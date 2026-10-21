package io.github.kxyr0.echoscript.api.syntax.statement;

import io.github.kxyr0.echoscript.api.syntax.SyntaxNode;

/**
 * Sealed immutable statement syntax contract.
 */
public sealed interface StatementSyntax extends SyntaxNode permits
        BlockStatementSyntax,
        IfStatementSyntax,
        LocalDeclarationStatementSyntax,
        ReturnStatementSyntax,
        SectionStatementSyntax,
        ExpressionStatementSyntax {
}
