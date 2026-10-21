package io.github.kxyr0.echowardrobe.core.binding;

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

/**
 * Syntax-only helpers for flat wearable intrinsic calls.
 */
public final class WearableCallSyntax {
    private WearableCallSyntax() {
    }

    public static Optional<InvocationExpressionSyntax> statementCall(StatementSyntax statement) {
        if (statement instanceof ExpressionStatementSyntax expression
                && expression.expression() instanceof InvocationExpressionSyntax invocation) {
            return Optional.of(invocation);
        }
        return Optional.empty();
    }

    public static String name(InvocationExpressionSyntax invocation) {
        ExpressionSyntax target = invocation.target();
        if (target instanceof NameExpressionSyntax name) {
            return name.name();
        }
        if (target instanceof MemberAccessExpressionSyntax access) {
            return access.memberName();
        }
        return "";
    }

    public static Optional<String> positionalString(InvocationExpressionSyntax invocation, int index) {
        return positional(invocation, index).flatMap(WearableCallSyntax::stringValue);
    }

    public static Optional<String> positionalSymbol(InvocationExpressionSyntax invocation, int index) {
        return positional(invocation, index).flatMap(WearableCallSyntax::symbolValue);
    }

    public static Optional<String> namedSymbol(InvocationExpressionSyntax invocation, String name) {
        return invocation.arguments().stream()
                .filter(argument -> argument.name().filter(name::equals).isPresent())
                .findFirst()
                .flatMap(WearableCallSyntax::symbolValue);
    }

    public static Optional<String> namedString(InvocationExpressionSyntax invocation, String name) {
        return invocation.arguments().stream()
                .filter(argument -> argument.name().filter(name::equals).isPresent())
                .findFirst()
                .flatMap(WearableCallSyntax::stringValue);
    }

    public static Optional<Double> positionalDouble(InvocationExpressionSyntax invocation, int index) {
        return positional(invocation, index)
                .map(ArgumentSyntax::value)
                .flatMap(WearableCallSyntax::doubleValue);
    }

    public static Optional<Integer> positionalInteger(InvocationExpressionSyntax invocation, int index) {
        return positional(invocation, index)
                .map(ArgumentSyntax::value)
                .flatMap(WearableCallSyntax::integerValue);
    }

    public static int argumentCount(InvocationExpressionSyntax invocation) {
        return invocation.arguments().size();
    }

    private static Optional<ArgumentSyntax> positional(InvocationExpressionSyntax invocation, int index) {
        if (index >= invocation.arguments().size()) {
            return Optional.empty();
        }
        return Optional.of(invocation.arguments().get(index));
    }

    private static Optional<String> stringValue(ArgumentSyntax argument) {
        if (argument.value() instanceof StringLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        return Optional.empty();
    }

    private static Optional<String> symbolValue(ArgumentSyntax argument) {
        if (argument.value() instanceof NameExpressionSyntax name) {
            return Optional.of(name.name());
        }
        return stringValue(argument);
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

    private static Optional<Integer> integerValue(ExpressionSyntax expression) {
        if (expression instanceof IntegerLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        return Optional.empty();
    }
}
