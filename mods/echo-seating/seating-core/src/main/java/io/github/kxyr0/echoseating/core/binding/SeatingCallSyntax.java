package io.github.kxyr0.echoseating.core.binding;

import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import java.util.Optional;

final class SeatingCallSyntax {
    private SeatingCallSyntax() {
    }

    static Optional<InvocationExpressionSyntax> statementCall(StatementSyntax statement) {
        if (statement instanceof ExpressionStatementSyntax expression
                && expression.expression() instanceof InvocationExpressionSyntax invocation) {
            return Optional.of(invocation);
        }
        return Optional.empty();
    }

    static String name(InvocationExpressionSyntax invocation) {
        ExpressionSyntax target = invocation.target();
        if (target instanceof NameExpressionSyntax name) {
            return name.name();
        }
        if (target instanceof MemberAccessExpressionSyntax access) {
            return access.memberName();
        }
        return "";
    }

    static Optional<String> positionalString(InvocationExpressionSyntax invocation, int index) {
        return positional(invocation, index).flatMap(argument -> stringValue(argument.value()));
    }

    static Optional<String> positionalName(InvocationExpressionSyntax invocation, int index) {
        return positional(invocation, index).flatMap(argument -> nameValue(argument.value()));
    }

    static Optional<Double> positionalDouble(InvocationExpressionSyntax invocation, int index) {
        return positional(invocation, index).flatMap(argument -> doubleValue(argument.value()));
    }

    static Optional<InvocationExpressionSyntax> positionalInvocation(
            InvocationExpressionSyntax invocation,
            int index) {
        return positional(invocation, index)
                .map(ArgumentSyntax::value)
                .filter(InvocationExpressionSyntax.class::isInstance)
                .map(InvocationExpressionSyntax.class::cast);
    }

    static Optional<String> positionalText(InvocationExpressionSyntax invocation, int index) {
        return positionalString(invocation, index).or(() -> positionalName(invocation, index));
    }

    private static Optional<ArgumentSyntax> positional(InvocationExpressionSyntax invocation, int index) {
        if (index >= invocation.arguments().size()) {
            return Optional.empty();
        }
        ArgumentSyntax argument = invocation.arguments().get(index);
        return argument.name().isEmpty() ? Optional.of(argument) : Optional.empty();
    }

    private static Optional<String> stringValue(ExpressionSyntax expression) {
        if (expression instanceof StringLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        return Optional.empty();
    }

    private static Optional<String> nameValue(ExpressionSyntax expression) {
        if (expression instanceof NameExpressionSyntax name) {
            return Optional.of(name.name());
        }
        return Optional.empty();
    }

    private static Optional<Double> doubleValue(ExpressionSyntax expression) {
        if (expression instanceof IntegerLiteralExpressionSyntax literal) {
            return Optional.of((double) literal.value());
        }
        if (expression instanceof DecimalLiteralExpressionSyntax literal) {
            return Optional.of(literal.value().doubleValue());
        }
        return Optional.empty();
    }
}
