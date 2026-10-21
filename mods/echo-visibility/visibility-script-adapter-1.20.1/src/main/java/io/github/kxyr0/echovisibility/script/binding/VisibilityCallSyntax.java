package io.github.kxyr0.echovisibility.script.binding;

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

public final class VisibilityCallSyntax {
    private VisibilityCallSyntax() {
    }

    public static Optional<InvocationExpressionSyntax> statementCall(StatementSyntax statement) {
        if (statement instanceof ExpressionStatementSyntax expression
                && expression.expression() instanceof InvocationExpressionSyntax invocation) {
            return Optional.of(invocation);
        }
        return Optional.empty();
    }

    public static String name(InvocationExpressionSyntax invocation) {
        if (invocation.target() instanceof NameExpressionSyntax name) {
            return name.name();
        }
        if (invocation.target() instanceof MemberAccessExpressionSyntax access) {
            return access.memberName();
        }
        return "";
    }

    public static Optional<ExpressionSyntax> positional(InvocationExpressionSyntax call, int index) {
        if (index < 0 || index >= call.arguments().size()) {
            return Optional.empty();
        }
        return Optional.of(call.arguments().get(index).value());
    }

    public static Optional<InvocationExpressionSyntax> positionalCall(InvocationExpressionSyntax call, int index) {
        return positional(call, index).filter(InvocationExpressionSyntax.class::isInstance)
                .map(InvocationExpressionSyntax.class::cast);
    }

    public static Optional<InvocationExpressionSyntax> namedCall(InvocationExpressionSyntax call, String name) {
        return named(call, name).filter(InvocationExpressionSyntax.class::isInstance)
                .map(InvocationExpressionSyntax.class::cast);
    }

    public static Optional<String> positionalText(InvocationExpressionSyntax call, int index) {
        return positional(call, index).flatMap(VisibilityCallSyntax::text);
    }

    public static Optional<String> namedText(InvocationExpressionSyntax call, String name) {
        return named(call, name).flatMap(VisibilityCallSyntax::text);
    }

    public static Optional<Double> positionalDouble(InvocationExpressionSyntax call, int index) {
        return positional(call, index).flatMap(VisibilityCallSyntax::number);
    }

    public static Optional<String> text(ExpressionSyntax expression) {
        if (expression instanceof StringLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        if (expression instanceof NameExpressionSyntax name) {
            return Optional.of(name.name());
        }
        return Optional.empty();
    }

    public static Optional<Double> number(ExpressionSyntax expression) {
        if (expression instanceof IntegerLiteralExpressionSyntax literal) {
            return Optional.of((double) literal.value());
        }
        if (expression instanceof DecimalLiteralExpressionSyntax literal) {
            return Optional.of(literal.value().doubleValue());
        }
        return Optional.empty();
    }

    private static Optional<ExpressionSyntax> named(InvocationExpressionSyntax call, String name) {
        return call.arguments().stream().filter(argument -> argument.name().filter(name::equals).isPresent())
                .map(ArgumentSyntax::value).findFirst();
    }
}
