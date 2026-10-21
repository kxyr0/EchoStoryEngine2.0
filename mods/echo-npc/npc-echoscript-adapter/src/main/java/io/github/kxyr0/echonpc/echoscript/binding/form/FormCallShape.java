package io.github.kxyr0.echonpc.echoscript.binding.form;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.BlockStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.LocalDeclarationStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ReturnStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;

final class FormCallShape {
    int countFormCalls(BlockStatementSyntax body) {
        int count = 0;
        for (StatementSyntax statement : body.statements()) {
            count += countStatement(statement);
        }
        return count;
    }

    boolean named(InvocationExpressionSyntax invocation, String name) {
        return invocation.target() instanceof NameExpressionSyntax target && name.equals(target.name());
    }

    private int countStatement(StatementSyntax statement) {
        if (statement instanceof ReturnStatementSyntax syntax && syntax.value().isPresent()) {
            return countExpression(syntax.value().orElseThrow());
        }
        if (statement instanceof ExpressionStatementSyntax syntax) {
            return countExpression(syntax.expression());
        }
        if (statement instanceof LocalDeclarationStatementSyntax syntax && syntax.initializer().isPresent()) {
            return countExpression(syntax.initializer().orElseThrow());
        }
        return 0;
    }

    private int countExpression(ExpressionSyntax expression) {
        if (!(expression instanceof InvocationExpressionSyntax invocation)) {
            return 0;
        }
        int own = named(invocation, "Form") ? 1 : 0;
        return own + invocation.arguments().stream().mapToInt(this::countArgument).sum();
    }

    private int countArgument(ArgumentSyntax argument) {
        return countExpression(argument.value());
    }
}
