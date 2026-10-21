package io.github.kxyr0.echoscript.api.syntax.expression;

import io.github.kxyr0.echoscript.api.syntax.SyntaxNode;

/**
 * Sealed immutable expression syntax contract.
 */
public sealed interface ExpressionSyntax extends SyntaxNode permits
        NameExpressionSyntax,
        StringLiteralExpressionSyntax,
        IntegerLiteralExpressionSyntax,
        DecimalLiteralExpressionSyntax,
        BooleanLiteralExpressionSyntax,
        MemberAccessExpressionSyntax,
        InvocationExpressionSyntax,
        ObjectCreationExpressionSyntax,
        ArrayLiteralExpressionSyntax,
        ParenthesizedExpressionSyntax {
}
